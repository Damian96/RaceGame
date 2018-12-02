
public class Tile {

	private int x;
	private int y;
	private int gridIndex;
	private String code;

	public Tile(int x, int y, int gridIndex) throws InvalidTileCoordException {
		if (x < 0)
			throw new InvalidTileCoordException("Invalid Tile x: " + x);
		else if(y < 0)
			throw new InvalidTileCoordException("Invalid Tile y: " + y);
		this.x = x;
		this.y = y;
		this.gridIndex = gridIndex;
		this.code = generateCode(x, y);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getGridIndex() {
		return gridIndex;
	}

	public String generateCode(int x, int y) {
		return Grid.alphabet[x] + String.valueOf(y + 1);
	}

	public Tile getNextTileAt(int offset) throws TileIndexOutOfBoundsException {
		return Grid.getTile(gridIndex + offset);
	}

	public String getCode() {
		return code;
	}

	@Override
	public String toString() {
		return "Tile [x=" + x + ", y=" + y + ", toString()=" + super.toString() + "]";
	}

}
