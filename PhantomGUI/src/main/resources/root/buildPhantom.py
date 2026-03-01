import re
import os
import numpy as np
import csv
from shapely.geometry import Polygon, Point

# ==================================================
# REGEX dùng để bắt số trong file VRML
# (int, float, scientific notation)
# ==================================================
_NUMBER_RE = r'[-+]?\d*\.\d+(?:[eE][-+]?\d+)?|[-+]?\d+(?:[eE][-+]?\d+)?'


# ==================================================
# Đọc file text với nhiều encoding khác nhau
# (VRML thường không đồng nhất encoding)
# ==================================================
def _read_text_with_encodings(path):
    encs = ["utf-8", "utf-8-sig", "utf-16", "latin-1"]
    for e in encs:
        try:
            with open(path, "r", encoding=e) as f:
                return f.read()
        except UnicodeDecodeError:
            continue
    raise ValueError(f"Không đọc được file {path} với encoding phổ biến.")


# ==================================================
# Tìm vị trí dấu } khớp với dấu { tại open_idx
# ==================================================
def _find_matching_brace(text, open_idx):
    if text[open_idx] != '{':
        raise ValueError("open_idx không trỏ tới '{'")
    depth = 1
    i = open_idx + 1
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("Không tìm thấy ngoặc đóng tương ứng.")


# ==================================================
# Parse danh sách point [x y z]
# ==================================================
def _parse_points(points_text):
    nums = re.findall(_NUMBER_RE, points_text)
    vals = [float(x) for x in nums]
    pts = []
    for i in range(0, len(vals), 3):
        if i + 2 < len(vals):
            pts.append((vals[i], vals[i+1], vals[i+2]))
    return pts


# ==================================================
# Parse coordIndex
# ==================================================
def _parse_coordindex(coord_text):
    ints = [int(x) for x in re.findall(r'-?\d+', coord_text)]
    groups = []
    cur = []
    for v in ints:
        if v == -1:
            if cur:
                groups.append(cur)
                cur = []
        else:
            cur.append(v)
    if cur:
        groups.append(cur)
    return groups


# ==================================================
# Đọc 1 file VRML
# ==================================================
def read_vrml(filename):
    content = _read_text_with_encodings(filename)

    roi_header = re.search(r'DEF\s+"([^"]+)"\s+Transform\s*\{', content)
    if not roi_header:
        return {"rois": []}

    roi_name = roi_header.group(1)

    open_idx = content.find('{', roi_header.start())
    close_idx = _find_matching_brace(content, open_idx)
    roi_block = content[open_idx+1:close_idx]

    t_match = re.search(r'translation\s+([^\n]+)', roi_block)
    if t_match:
        nums = [float(x) for x in re.findall(_NUMBER_RE, t_match.group(1))[:3]]
        while len(nums) < 3:
            nums.append(0.0)
        roi_translation = tuple(nums)
    else:
        roi_translation = (0.0, 0.0, 0.0)

    trace_pattern = re.compile(
        r'DEF\s+"([^"]+)"\s+Transform\s*\{'
        r'.*?translation\s+([^\n]+)'
        r'.*?Coordinate\s*{\s*point\s*\[([^\]]+)\]'
        r'.*?coordIndex\s*\[([^\]]+)\]',
        re.S
    )

    traces = []
    for m in trace_pattern.finditer(roi_block):
        trace_name = m.group(1)

        tnums = [float(x) for x in re.findall(_NUMBER_RE, m.group(2))[:3]]
        while len(tnums) < 3:
            tnums.append(0.0)
        trace_translation = tuple(tnums)

        points = _parse_points(m.group(3))
        coordindex = _parse_coordindex(m.group(4))

        points = [
            (
                x + roi_translation[0] + trace_translation[0],
                y + roi_translation[1] + trace_translation[1],
                z + roi_translation[2] + trace_translation[2]
            )
            for x, y, z in points
        ]

        polygons = []
        for group in coordindex:
            if len(group) >= 3:
                poly_coords = [(points[i][0], points[i][1]) for i in group if i < len(points)]
                if len(poly_coords) >= 3:
                    polygons.append(Polygon(poly_coords))

        trace_z = roi_translation[2] + trace_translation[2]

        traces.append({
            "name": trace_name,
            "translation": trace_translation,
            "points": points,
            "coordIndex": coordindex,
            "polygons": polygons,
            "z": trace_z,
        })

    return {
        "rois": [{
            "name": roi_name,
            "translation": roi_translation,
            "traces": traces
        }]
    }


# ==================================================
# Kiểm tra voxel có nằm trong ROI không
# ==================================================
def point_in_roi(x, y, z, roi_traces):
    if not roi_traces:
        return False

    z_min_roi = min(tr["z"] for tr in roi_traces)
    z_max_roi = max(tr["z"] for tr in roi_traces)
    if z < z_min_roi or z > z_max_roi:
        return False

    nearest_trace = min(roi_traces, key=lambda tr: abs(tr["z"] - z))

    pt2d = Point(x, y)
    for poly in nearest_trace["polygons"]:
        if poly.contains(pt2d) or poly.touches(pt2d):
            return True

    return False


# ==================================================
# Format float
# ==================================================
def _format_float(val):
    if abs(val - int(val)) < 1e-6:
        return str(int(val))
    else:
        s = f"{val:.3f}".rstrip('0').rstrip('.')
        if s == '-0':
            return '0'
        return s


# ==================================================
# Ghi file G4DCM
# ==================================================
def write_g4dcm(filename, materials, structures,
                nx, ny, nz, xmin, xmax, ymin, ymax, zmin, zmax,
                mat_id, density_mat, struct_id,
                write_structure=True):

    with open(filename, "w") as f:
        f.write(f"{len(materials)}\n")
        for mid, name in materials.items():
            f.write(f"{mid} {name}\n")

        f.write(f"{nx} {ny} {nz}\n")
        f.write(f"{_format_float(xmin)} {_format_float(xmax)}\n")
        f.write(f"{_format_float(ymin)} {_format_float(ymax)}\n")
        f.write(f"{_format_float(zmin)} {_format_float(zmax)}\n")

        for k in range(nz):
            for j in range(ny):
                f.write(" ".join(str(mat_id[k, j, i]) for i in range(nx)) + "\n")

        for k in range(nz):
            for j in range(ny):
                f.write(" ".join(_format_float(density_mat[k, j, i]) for i in range(nx)) + "\n")

        if write_structure:
            for k in range(nz):
                for j in range(ny):
                    f.write(" ".join(str(struct_id[k, j, i]) for i in range(nx)) + "\n")
            for sid, name in structures.items():
                f.write(f"{sid} {name}\n")


# ==================================================
# Build voxel từ nhiều VRML
# ==================================================
def build_from_multiple_vrml_voxel_size(
        csv_file,
        voxel_size_x, voxel_size_y, voxel_size_z,
        output_file,
        write_structure=True):

    materials = {0: "G4_AIR"}
    structures = {}
    mat_counter = 1
    struct_counter = 1

    x_all, y_all, z_all = [], [], []
    with open(csv_file, newline='', encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            data = read_vrml(row["filename"])
            if not data["rois"]:
                continue
            for tr in data["rois"][0]["traces"]:
                for x, y, z in tr["points"]:
                    x_all.append(x)
                    y_all.append(y)
                    z_all.append(z)

    xmin_real, xmax_real = min(x_all), max(x_all)
    ymin_real, ymax_real = min(y_all), max(y_all)
    zmin_real, zmax_real = min(z_all), max(z_all)

    xmin = xmin_real - voxel_size_x / 2
    ymin = ymin_real - voxel_size_y / 2
    zmin = zmin_real - voxel_size_z / 2

    nx = int(np.ceil((xmax_real - xmin) / voxel_size_x))
    ny = int(np.ceil((ymax_real - ymin) / voxel_size_y))
    nz = int(np.ceil((zmax_real - zmin) / voxel_size_z))

    xmax = xmin + nx * voxel_size_x
    ymax = ymin + ny * voxel_size_y
    zmax = zmin + nz * voxel_size_z

    mat_id = np.zeros((nz, ny, nx), dtype=int)
    density_mat = np.zeros((nz, ny, nx), dtype=float)
    struct_id = np.zeros((nz, ny, nx), dtype=int)

    with open(csv_file, newline='', encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            vrml_file = row["filename"]
            mat_name = row["material_name"]
            density = float(row["density"])

            struct_name = os.path.splitext(os.path.basename(vrml_file))[0]

            data = read_vrml(vrml_file)
            if not data["rois"]:
                continue
            roi = data["rois"][0]

            zmin_roi = min(tr["z"] for tr in roi["traces"])
            zmax_roi = max(tr["z"] for tr in roi["traces"])

            if mat_name not in materials.values():
                materials[mat_counter] = mat_name
                material_id = mat_counter
                mat_counter += 1
            else:
                material_id = [k for k, v in materials.items() if v == mat_name][0]

            if struct_name not in structures.values():
                structures[struct_counter] = struct_name
                structure_id = struct_counter
                struct_counter += 1
            else:
                structure_id = [k for k, v in structures.items() if v == struct_name][0]

            xs = [xmin + voxel_size_x * (i + 0.5) for i in range(nx)]
            ys = [ymin + voxel_size_y * (j + 0.5) for j in range(ny)]
            zs = [zmin + voxel_size_z * (k + 0.5) for k in range(nz)]

            for k, z in enumerate(zs):
                if z < zmin_roi or z > zmax_roi:
                    continue
                for j, y in enumerate(ys):
                    for i, x in enumerate(xs):
                        if point_in_roi(x, y, z, roi["traces"]) and mat_id[k, j, i] == 0:
                            mat_id[k, j, i] = material_id
                            density_mat[k, j, i] = density
                            struct_id[k, j, i] = structure_id

    # ==================================================
    # XOAY PHANTOM 180° QUANH TRỤC Z (CHỈ LẬT MA TRẬN)
    # ==================================================
    mat_id = mat_id[:, ::-1, ::-1]
    density_mat = density_mat[:, ::-1, ::-1]
    struct_id = struct_id[:, ::-1, ::-1]

    write_g4dcm(
        output_file,
        materials, structures,
        nx, ny, nz,
        xmin, xmax, ymin, ymax, zmin, zmax,
        mat_id, density_mat, struct_id,
        write_structure=write_structure
    )


# ==================================================
# Ví dụ chạy
# ==================================================
if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Build voxel phantom from VRML files")
    parser.add_argument("--csv",      required=True,  help="Path to mapping CSV file")
    parser.add_argument("--voxel_x",  type=float, default=1.0, help="Voxel size X (mm)")
    parser.add_argument("--voxel_y",  type=float, default=1.0, help="Voxel size Y (mm)")
    parser.add_argument("--voxel_z",  type=float, default=1.0, help="Voxel size Z (mm)")
    parser.add_argument("--output",   required=True,  help="Output .g4dcm file path")
    parser.add_argument("--write_structure", action="store_true", default=False)
    args = parser.parse_args()

    build_from_multiple_vrml_voxel_size(
        csv_file=args.csv,
        voxel_size_x=args.voxel_x,
        voxel_size_y=args.voxel_y,
        voxel_size_z=args.voxel_z,
        output_file=args.output,
        write_structure=args.write_structure
    )
