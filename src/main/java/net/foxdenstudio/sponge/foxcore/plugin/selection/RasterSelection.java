package net.foxdenstudio.sponge.foxcore.plugin.selection;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox3;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 9/14/2016.
 */
public class RasterSelection implements ISelection {

    private final HashSet<Vector3i> positions;
    private BoundingBox3 bounds;
    private boolean dirty = true;

    public RasterSelection() {
        positions = new HashSet<>();
    }

    public RasterSelection(HashSet<Vector3i> positions) {
        this.positions = positions;
    }

    public static void main(String[] args) {
        HashSet<Vector3i> set = new HashSet<>();
        //Random random = new Random();
        System.out.println("Generating values");
        int x = 0;
        for (int i = 0; i < 50000000; i++) {
            set.add(new Vector3i(x++, x++, x++));
        }
        RasterSelection selection = new RasterSelection(set);
        System.out.println("Starting calculation");
        long start = System.currentTimeMillis();
        selection.calculateBounds();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));

        try (DB db = DBMaker.memoryDB().make()) {

        }
    }

    @Override
    public Text overview() {
        return Text.EMPTY;
    }

    @Override
    public Optional<Text> details() {
        return Optional.empty();
    }

    @Override
    public String type() {
        return "raster";
    }

    @Override
    public int size() {
        return positions.size();
    }

    @Override
    public Optional<BoundingBox3> bounds() {
        calculateBounds();
        return Optional.ofNullable(bounds);
    }

    @Override
    public Iterator<Vector3i> iterator() {
        return positions.iterator();
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return positions.contains(new Vector3i(x, y, z));
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return positions.contains(new Vector3i(x, y, z));
    }

    @Override
    public boolean contains(Vector3i vec) {
        return positions.contains(vec);
    }

    @Override
    public boolean contains(Vector3d vec) {
        return positions.contains(vec.toInt());
    }

    private void calculateBounds() {
        if (dirty) {
            if (this.positions.isEmpty()) {
                this.bounds = null;
            } else {
                int
                        minX = Integer.MAX_VALUE,
                        minY = Integer.MAX_VALUE,
                        minZ = Integer.MAX_VALUE,
                        maxX = Integer.MIN_VALUE,
                        maxY = Integer.MIN_VALUE,
                        maxZ = Integer.MIN_VALUE;

                for (Vector3i vec : this.positions) {
                    int x = vec.getX(), y = vec.getY(), z = vec.getZ();
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
                this.bounds = new BoundingBox3(
                        new Vector3i(minX, minY, minZ),
                        new Vector3i(maxX, maxY, maxZ)
                );
            }
            dirty = false;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.positions.isEmpty();
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        return null;
    }
}
