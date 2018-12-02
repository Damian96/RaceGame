import java.util.Random;

public class GreyTile extends Tile {

	private int fuelCost;

	public GreyTile(int x, int y, int gridIndex) throws InvalidTileCoordException {
		super(x, y, gridIndex);
		fuelCost = new Random().nextInt(3) + 1;
	}

	public int getFuelCost() {
		return fuelCost;
	}

	@Override
	public String toString() {
		return "GreyTile [fuelCost=" + fuelCost + ", toString()=" + super.toString() + "]";
	}

}
