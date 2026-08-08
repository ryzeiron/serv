"""Petite bibliotheque partagee pour generer des structures NBT vanilla Minecraft
(memes conventions que build_plaza.py : toile 176x222 non concernee ici, ce sont
des structures de monde, pas des textures gui)."""
from nbtlib import Compound, List, Int, String
from nbtlib import File


class StructureBuilder:
    def __init__(self, size_x, size_y, size_z):
        self.size_x = size_x
        self.size_y = size_y
        self.size_z = size_z
        self.palette = []
        self.palette_index = {}
        self.blocks = {}

    def block_state(self, name, **props):
        key = (name, tuple(sorted(props.items())))
        if key in self.palette_index:
            return self.palette_index[key]
        entry = Compound({"Name": String(name)})
        if props:
            entry["Properties"] = Compound({k: String(str(v)) for k, v in props.items()})
        self.palette.append(entry)
        idx = len(self.palette) - 1
        self.palette_index[key] = idx
        return idx

    def set_block(self, x, y, z, name, **props):
        if not (0 <= x < self.size_x and 0 <= y < self.size_y and 0 <= z < self.size_z):
            raise ValueError(f"Hors limites: {(x, y, z)} (taille {(self.size_x, self.size_y, self.size_z)})")
        self.blocks[(x, y, z)] = self.block_state(name, **props)

    def fill(self, x1, y1, z1, x2, y2, z2, name, **props):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set_block(x, y, z, name, **props)

    def fill_disk(self, cx, y, cz, radius, name, **props):
        r2 = radius * radius
        for x in range(self.size_x):
            for z in range(self.size_z):
                dx, dz = x - cx, z - cz
                if dx * dx + dz * dz <= r2:
                    self.set_block(x, y, z, name, **props)

    def ring(self, cx, y, cz, inner_radius, outer_radius, name, **props):
        inner2, outer2 = inner_radius * inner_radius, outer_radius * outer_radius
        for x in range(self.size_x):
            for z in range(self.size_z):
                dx, dz = x - cx, z - cz
                d2 = dx * dx + dz * dz
                if inner2 <= d2 <= outer2:
                    self.set_block(x, y, z, name, **props)

    def save(self, path, data_version=3465):
        blocks_list = List[Compound]([
            Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(state)})
            for (x, y, z), state in sorted(self.blocks.items())
        ])
        structure = Compound({
            "DataVersion": Int(data_version),
            "size": List[Int]([Int(self.size_x), Int(self.size_y), Int(self.size_z)]),
            "entities": List[Compound]([]),
            "blocks": blocks_list,
            "palette": List[Compound](self.palette),
        })
        File(structure, gzipped=True).save(path)
        return len(self.blocks), len(self.palette)


def verify(path):
    from nbtlib import load
    nbt = load(path)
    palette = nbt["palette"]
    blocks = nbt["blocks"]
    sx, sy, sz = [int(v) for v in nbt["size"]]
    max_idx = max((int(b["state"]) for b in blocks), default=-1)
    assert max_idx < len(palette), "index hors palette"
    positions = [tuple(int(v) for v in b["pos"]) for b in blocks]
    assert len(positions) == len(set(positions)), "positions dupliquees"
    for (x, y, z) in positions:
        assert 0 <= x < sx and 0 <= y < sy and 0 <= z < sz, f"hors limites: {(x, y, z)}"
    return {"size": (sx, sy, sz), "blocks": len(blocks), "palette": len(palette)}


def ascii_top(path, symbol_map, default="?"):
    from nbtlib import load
    nbt = load(path)
    palette = [str(p["Name"]).replace("minecraft:", "") for p in nbt["palette"]]
    blocks = nbt["blocks"]
    sx, sy, sz = [int(v) for v in nbt["size"]]
    top = {}
    for b in blocks:
        x, y, z = [int(v) for v in b["pos"]]
        name = palette[int(b["state"])]
        key = (x, z)
        if key not in top or y >= top[key][0]:
            top[key] = (y, name)
    lines = []
    for z in range(sz):
        row = ""
        for x in range(sx):
            if (x, z) in top:
                row += symbol_map.get(top[(x, z)][1], default)
            else:
                row += " "
        lines.append(row)
    return "\n".join(lines)
