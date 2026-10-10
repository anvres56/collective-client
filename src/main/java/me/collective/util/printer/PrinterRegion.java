package me.collective.util.printer;

/**
 * Прямоугольная область (AABB) схемы в координатах мира (порт из BepHax).
 */
public record PrinterRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }
}