import numpy as np 

# ==================================================
# Đọc phantom từ file G4DCM
# ==================================================
def read_g4dcm(path, verbose=False):
    with open(path, 'r') as f:
        lines = [line.strip() for line in f if line.strip()]
    idx = 0

    nmat = int(lines[idx]); idx += 1
    materials = {}
    for _ in range(nmat):
        mid, name = lines[idx].split(maxsplit=1)
        materials[int(mid)] = name.strip('"')
        idx += 1

    if idx < len(lines) and lines[idx] == "HFS":
        idx += 1

    nx, ny, nz = map(int, lines[idx].split())
    idx += 1

    xmin, xmax = map(float, lines[idx].split()); idx += 1
    ymin, ymax = map(float, lines[idx].split()); idx += 1
    zmin, zmax = map(float, lines[idx].split()); idx += 1

    mat_ids = []
    for _ in range(nz * ny):
        mat_ids.append(list(map(int, lines[idx].split())))
        idx += 1
    mat_ids = np.array(mat_ids).reshape((nz, ny, nx))

    density_vals = []
    while len(density_vals) < nz * ny * nx:
        density_vals.extend(map(float, lines[idx].split()))
        idx += 1
    density = np.array(density_vals).reshape((nz, ny, nx))

    struct_ids = None
    if idx < len(lines) and all(p.replace('-', '').isdigit() for p in lines[idx].split()):
        struct = []
        for _ in range(nz * ny):
            struct.append(list(map(int, lines[idx].split())))
            idx += 1
        struct_ids = np.array(struct).reshape((nz, ny, nx))

    struct_dict = None
    if struct_ids is not None and idx < len(lines):
        struct_dict = {}
        while idx < len(lines):
            sid, name = lines[idx].split(maxsplit=1)
            struct_dict[int(sid)] = name.strip('"')
            idx += 1

    return dict(
        dims=(nx, ny, nz),
        xmin=xmin, xmax=xmax,
        ymin=ymin, ymax=ymax,
        zmin=zmin, zmax=zmax,
        mat_ids=mat_ids,
        density=density,
        struct_ids=struct_ids,
        mat_dict=materials,
        struct_dict=struct_dict
    )

# ==================================================
# Xoay array (bội số 90°)
# ==================================================
def rotate_array(arr, kx=0, ky=0, kz=0):
    if arr is None:
        return None

    arr = arr.copy()

    for _ in range(kx % 4):
        arr = np.rot90(arr, axes=(1,0))

    for _ in range(ky % 4):
        arr = np.rot90(arr, axes=(2,0))

    for _ in range(kz % 4):
        arr = np.rot90(arr, axes=(2,1))

    return arr

# ==================================================
# Merge dictionary + update ID array
# ==================================================
def merge_dicts(d1, d2, a1, a2, start_id=0):
    new_d = {}
    name2id = {}
    nid = start_id

    na1 = a1.copy()
    for oid, name in d1.items():
        if name not in name2id:
            name2id[name] = nid
            new_d[nid] = name
            nid += 1
        na1[a1 == oid] = name2id[name]

    na2 = a2.copy()
    for oid, name in d2.items():
        if name not in name2id:
            name2id[name] = nid
            new_d[nid] = name
            nid += 1
        na2[a2 == oid] = name2id[name]

    return new_d, na1, na2

# ==================================================
# Xử lý từng tư thế
# ==================================================
def handle_face_to_face(mat1, rho1, s1, mat2, rho2, s2, d):

    mat1 = rotate_array(mat1, kz=2)
    rho1 = rotate_array(rho1, kz=2)
    if s1 is not None:
        s1 = rotate_array(s1, kz=2)
    if s2 is not None:
        s2 = rotate_array(s2, kz=2)

    nz1, ny1, nx1 = mat1.shape
    nz2, ny2, nx2 = mat2.shape

    ox1 = oy1 = oz1 = 0
    ox2 = (nx1 - nx2) // 2
    oy2 = ny1 + d
    oz2 = 0

    return mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2


def handle_front_to_back(mat1, rho1, s1, mat2, rho2, s2, d):

    nz1, ny1, nx1 = mat1.shape
    nz2, ny2, nx2 = mat2.shape

    ox1 = oy1 = oz1 = 0
    ox2 = (nx1 - nx2) // 2
    oy2 = ny1 + d
    oz2 = 0

    return mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2


def handle_side_by_side(mat1, rho1, s1, mat2, rho2, s2, d):

    nz1, ny1, nx1 = mat1.shape

    ox1 = oy1 = oz1 = 0
    ox2 = nx1 + d
    oy2 = 0
    oz2 = 0

    return mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2


def handle_standing_supine(mat1, rho1, s1, mat2, rho2, s2, d):

    mat1 = rotate_array(mat1, ky=1)
    rho1 = rotate_array(rho1, ky=1)
    if s1 is not None:
        s1 = rotate_array(s1, ky=1)

    mat1 = rotate_array(mat1, kx=3)
    rho1 = rotate_array(rho1, kx=3)
    if s1 is not None:
        s1 = rotate_array(s1, kx=3)

    nz1, ny1, nx1 = mat1.shape
    nz2, ny2, nx2 = mat2.shape

    ox1 = oy1 = oz1 = 0
    ox2 = (nx1 // 2) - (nx2 // 2)
    oy2 = ny1 + d
    oz2 = (nz1 // 2) - (nz2 // 2)

    return mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2

# ==================================================
# Ghép phantom
# ==================================================
def merge_phantoms(file1, file2,
                   position="face_to_face",
                   d=0,
                   verbose=False):

    ph1 = read_g4dcm(file1)
    ph2 = read_g4dcm(file2)

    dx = (ph1['xmax'] - ph1['xmin']) / ph1['dims'][0]
    dy = (ph1['ymax'] - ph1['ymin']) / ph1['dims'][1]
    dz = (ph1['zmax'] - ph1['zmin']) / ph1['dims'][2]

    mat_dict, mat1, mat2 = merge_dicts(
        ph1['mat_dict'], ph2['mat_dict'],
        ph1['mat_ids'], ph2['mat_ids']
    )

    rho1 = ph1['density']
    rho2 = ph2['density']

    # ===== STRUCT LOGIC =====
    if (ph1['struct_ids'] is not None and ph2['struct_ids'] is not None and
        ph1['struct_dict'] is not None and ph2['struct_dict'] is not None):

        struct_dict, s1, s2 = merge_dicts(
            ph1['struct_dict'], ph2['struct_dict'],
            ph1['struct_ids'], ph2['struct_ids']
        )
        use_struct = True
    else:
        struct_dict, s1, s2 = None, None, None
        use_struct = False

    # ===== POSITION =====
    if position == "face_to_face":
        mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2 = \
            handle_face_to_face(mat1, rho1, s1, mat2, rho2, s2, d)

    elif position == "front_to_back":
        mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2 = \
            handle_front_to_back(mat1, rho1, s1, mat2, rho2, s2, d)

    elif position == "side_by_side":
        mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2 = \
            handle_side_by_side(mat1, rho1, s1, mat2, rho2, s2, d)

    elif position == "standing_supine":
        mat1, rho1, s1, mat2, rho2, s2, ox1, oy1, oz1, ox2, oy2, oz2 = \
            handle_standing_supine(mat1, rho1, s1, mat2, rho2, s2, d)

    else:
        raise ValueError("Position không hợp lệ")

    nz1, ny1, nx1 = mat1.shape
    nz2, ny2, nx2 = mat2.shape

    minx = min(ox1, ox2)
    miny = min(oy1, oy2)
    minz = min(oz1, oz2)

    if minx < 0:
        ox1 -= minx
        ox2 -= minx
    if miny < 0:
        oy1 -= miny
        oy2 -= miny
    if minz < 0:
        oz1 -= minz
        oz2 -= minz

    nx_new = max(ox1 + nx1, ox2 + nx2)
    ny_new = max(oy1 + ny1, oy2 + ny2)
    nz_new = max(oz1 + nz1, oz2 + nz2)

    new_mat = np.zeros((nz_new, ny_new, nx_new), dtype=int)
    new_rho = np.zeros((nz_new, ny_new, nx_new), dtype=float)

    new_mat[oz1:oz1+nz1, oy1:oy1+ny1, ox1:ox1+nx1] = mat1
    new_rho[oz1:oz1+nz1, oy1:oy1+ny1, ox1:ox1+nx1] = rho1

    new_mat[oz2:oz2+nz2, oy2:oy2+ny2, ox2:ox2+nx2] = mat2
    new_rho[oz2:oz2+nz2, oy2:oy2+ny2, ox2:ox2+nx2] = rho2

    if use_struct:
        new_struct = np.zeros((nz_new, ny_new, nx_new), dtype=int)
        new_struct[oz1:oz1+nz1, oy1:oy1+ny1, ox1:ox1+nx1] = s1
        new_struct[oz2:oz2+nz2, oy2:oy2+ny2, ox2:ox2+nx2] = s2
    else:
        new_struct = None

    return dict(
        dims=(nx_new, ny_new, nz_new),
        xmin=0, xmax=nx_new * dx,
        ymin=0, ymax=ny_new * dy,
        zmin=0, zmax=nz_new * dz,
        mat_ids=new_mat,
        density=new_rho,
        struct_ids=new_struct,
        mat_dict=mat_dict,
        struct_dict=struct_dict
    )

# ==================================================
# Ghi G4DCM
# ==================================================
def write_g4dcm(ph, path):
    with open(path, "w") as f:
        f.write(f"{len(ph['mat_dict'])}\n")
        for i, n in sorted(ph['mat_dict'].items()):
            f.write(f"{i} \"{n}\"\n")

        nx, ny, nz = ph['dims']
        f.write(f"{nx} {ny} {nz}\n")

        f.write(f"{ph['xmin']} {ph['xmax']}\n")
        f.write(f"{ph['ymin']} {ph['ymax']}\n")
        f.write(f"{ph['zmin']} {ph['zmax']}\n")

        mat = ph['mat_ids'].reshape(nz * ny, nx)
        for r in mat:
            f.write(" ".join(map(str, r)) + "\n")

        rho = ph['density'].reshape(nz * ny, nx)
        for r in rho:
            f.write(" ".join(f"{v:.3f}" for v in r) + "\n")

        if ph['struct_ids'] is not None:
            struct = ph['struct_ids'].reshape(nz * ny, nx)
            for r in struct:
                f.write(" ".join(map(str, r)) + "\n")

            for i, n in sorted(ph['struct_dict'].items()):
                f.write(f"{i} \"{n}\"\n")


# ==================================================
# Entry point (GIỮ NGUYÊN)
# ==================================================
if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Merge two G4DCM phantom files")
    parser.add_argument("--input1",     required=True,  help="Path to first .g4dcm file")
    parser.add_argument("--input2",     required=True,  help="Path to second .g4dcm file")
    parser.add_argument("--situation",  required=True,
                        choices=["face_to_face", "side_by_side", "front_to_back", "standing_supine"],
                        help="Relative situation of the two phantoms")
    parser.add_argument("--separation", required=True,  type=int, help="Separation in voxels")
    parser.add_argument("--output",     required=True,  help="Output .g4dcm file path")
    args = parser.parse_args()

    merged = merge_phantoms(
        args.input1,
        args.input2,
        position=args.situation,
        d=args.separation,
        verbose=True
    )

    write_g4dcm(merged, args.output)
    print(f"✔ Đã ghép xong phantom → {args.output}")
