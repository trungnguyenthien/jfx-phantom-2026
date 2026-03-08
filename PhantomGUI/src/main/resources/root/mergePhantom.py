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
    for _ in range(kx % 4):
        arr = np.rot90(arr, axes=(1, 0))   # Y-Z  (xoay quanh X)
    for _ in range(ky % 4):
        arr = np.rot90(arr, axes=(2, 0))   # X-Z  (xoay quanh Y)
    for _ in range(kz % 4):
        arr = np.rot90(arr, axes=(2, 1))   # X-Y  (xoay quanh Z)
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
# Ghép phantom theo tư thế giao tiếp
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
    ph1['mat_ids'] = mat1
    ph2['mat_ids'] = mat2
    ph1['mat_dict'] = ph2['mat_dict'] = mat_dict

    # ================= XOAY THEO TƯ THẾ =================
    if position == "face_to_face":
        mat2 = rotate_array(ph2['mat_ids'], kz=2)
        rho2 = rotate_array(ph2['density'], kz=2)

    elif position in ("front_to_back", "side_by_side"):
        mat2 = ph2['mat_ids']
        rho2 = ph2['density']

    elif position == "standing_beside_supine":
        # === FIX VẤN ĐỀ 2 ===
        # Phantom 2: nằm NGỬA
        # Ban đầu mặt hướng -Y → cần +Z
        # => xoay -90° quanh X => kx = 3
        mat2 = rotate_array(ph2['mat_ids'], kx=3)
        rho2 = rotate_array(ph2['density'], kx=3)

        # Phantom 1: đứng, quay mặt sang bên để nhìn phantom nằm
        ph1['mat_ids'] = rotate_array(ph1['mat_ids'], kz=1)
        ph1['density'] = rotate_array(ph1['density'], kz=1)

    else:
        raise ValueError("Position không hợp lệ")

    nz1, ny1, nx1 = ph1['mat_ids'].shape
    nz2, ny2, nx2 = mat2.shape

    # ================= OFFSET =================
    if position == "side_by_side":
        ox, oy, oz = nx1 + d, 0, 0

    elif position in ("face_to_face", "front_to_back"):
        ox, oy, oz = 0, ny1 + d, 0

    else:  # standing_beside_supine
        ox = nx1 + d
        oy = ny2 // 4
        oz = (nz1 // 2) - (nz2 // 2)

    nx_new = max(nx1, ox + nx2)
    ny_new = max(ny1, oy + ny2)
    nz_new = max(nz1, oz + nz2, nz1)

    new_mat = np.zeros((nz_new, ny_new, nx_new), dtype=int)
    new_rho = np.zeros((nz_new, ny_new, nx_new), dtype=float)

    new_mat[:nz1, :ny1, :nx1] = ph1['mat_ids']
    new_rho[:nz1, :ny1, :nx1] = ph1['density']

    new_mat[oz:oz+nz2, oy:oy+ny2, ox:ox+nx2] = mat2
    new_rho[oz:oz+nz2, oy:oy+ny2, ox:ox+nx2] = rho2

    return dict(
        dims=(nx_new, ny_new, nz_new),
        xmin=0, xmax=nx_new * dx,
        ymin=0, ymax=ny_new * dy,
        zmin=0, zmax=nz_new * dz,
        mat_ids=new_mat,
        density=new_rho,
        struct_ids=None,
        mat_dict=mat_dict,
        struct_dict=None
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
        f.write(f"0 {nx}\n0 {ny}\n0 {nz}\n")

        mat = ph['mat_ids'].reshape(nz * ny, nx)
        for r in mat:
            f.write(" ".join(map(str, r)) + "\n")

        rho = ph['density'].reshape(nz * ny, nx)
        for r in rho:
            f.write(" ".join(f"{v:.3f}" for v in r) + "\n")

# ==================================================
# Ví dụ
# ==================================================
if __name__ == "__main__":
    merged = merge_phantoms(
        "patient.g4dcm",
        "output.g4dcm",
        position="standing_beside_supine",
        d=20,
        verbose=True
    )

    write_g4dcm(merged, "phantom_merged.g4dcm")
    print("✔ Đã ghép xong phantom")
