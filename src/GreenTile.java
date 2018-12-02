
public class GreenTile extends Tile {

	public GreenTile(int x, int y, int gridIndex) throws InvalidTileCoordException {
		super(x, y, gridIndex);
	}

	@Override
	public String toString() {
		return "GreenTile [toString()=" + super.toString() + "]";
	}

}
