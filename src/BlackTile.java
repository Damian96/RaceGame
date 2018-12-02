
public class BlackTile extends Tile {

	public BlackTile(int x, int y, int gridIndex) throws InvalidTileCoordException {
		super(x, y, gridIndex);
	}

	@Override
	public String toString() {
		return "BlackTile [toString()=" + super.toString() + "]";
	}

}
