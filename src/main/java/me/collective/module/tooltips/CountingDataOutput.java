package me.collective.module.tooltips;

import java.io.DataOutput;

/**
 * Счётчик записанных байтов.
 *
 * <p>Порт {@code TooltipsDataOutput}: реализует {@link DataOutput} и считает
 * объём, вместо того чтобы что-то писать. Так размер предмета измеряется
 * ровно так же, как в оригинале, без промежуточного массива в памяти.
 */
public class CountingDataOutput implements DataOutput {

    private int count;

    public int count() {
        return count;
    }

    @Override
    public void write(int b) {
        count++;
    }

    @Override
    public void write(byte[] bytes) {
        count += bytes.length;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        count += length;
    }

    @Override
    public void writeBoolean(boolean value) {
        count++;
    }

    @Override
    public void writeByte(int value) {
        count++;
    }

    @Override
    public void writeShort(int value) {
        count += 2;
    }

    @Override
    public void writeChar(int value) {
        count += 2;
    }

    @Override
    public void writeInt(int value) {
        count += 4;
    }

    @Override
    public void writeLong(long value) {
        count += 8;
    }

    @Override
    public void writeFloat(float value) {
        count += 4;
    }

    @Override
    public void writeDouble(double value) {
        count += 8;
    }

    @Override
    public void writeChars(String value) {
        count += value.length() * 2;
    }

    @Override
    public void writeBytes(String value) {
        count += value.length();
    }

    @Override
    public void writeUTF(String value) {
        // точную длину префикса считать не нужно: для оценки размера хватает
        // длины строки в UTF-8 плюс два байта на префикс
        count += value.length() + 2;
    }

    /** В оригинале эти методы тоже есть, но они не часть DataOutput. */
    public void write(int[] values) {
        count += values.length * 4;
    }

    /** В оригинале эти методы тоже есть, но они не часть DataOutput. */
    public void write(long[] values) {
        count += values.length * 8;
    }
}