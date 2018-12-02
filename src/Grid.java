import java.util.ArrayList;

public class Grid {

	public static final int WIDTH = 10;
	public static final int HEIGHT = 10;
	public static final int SIZE = WIDTH * HEIGHT - 1;
	private final static int STARTING_FUEL = 120;
	private static ArrayList<Tile> tiles;
	private static final int DIFFICULTY_EASY = 1;
	private static final int DIFFICULTY_NORMAL = 2;
	private static final int DIFFICULTY_HARD = 3;
	private static final int SPECIAL_TILE_LOWER_LIMIT = 5;
	private static final int SPECIAL_TILE_UPPER_LIMIT = WIDTH * HEIGHT - 6;
	public static final String[] alphabet = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
	private static int difficulty;

	public Grid(int difficulty) throws InvalidDifficultyException, InvalidTileCoordException {
		Grid.difficulty = difficulty;
		tiles = new ArrayList<Tile>(WIDTH * HEIGHT);

		for (int h = 0; h < HEIGHT; h++) {
			for (int w = 0; w < WIDTH; w++) {
				tiles.add(new GreyTile(w, h, tiles.size()));
			}
		}

		if (difficulty == DIFFICULTY_EASY) {
			addSpecialTiles(RaceGame.GREEN_TILE_RATE * 1.5, "GreenTile");
			addSpecialTiles(RaceGame.BLACK_TILE_RATE / 2, "BlackTile");
		} else if (difficulty == DIFFICULTY_NORMAL) {
			addSpecialTiles(RaceGame.GREEN_TILE_RATE, "GreenTile");
			addSpecialTiles(RaceGame.BLACK_TILE_RATE, "BlackTile");
		} else if (difficulty == DIFFICULTY_HARD) {
			addSpecialTiles(RaceGame.GREEN_TILE_RATE / 2, "GreenTile");
			addSpecialTiles(RaceGame.BLACK_TILE_RATE, "BlackTile");
		} else {
			throw new InvalidDifficultyException("Invalid game difficulty code: " + difficulty);
		}
	}

	private void addSpecialTiles(double rate, String className) throws InvalidTileCoordException {
		int counter = 0;
		int random;

		do {

			random = RaceGame.getRandomNumber(WIDTH * HEIGHT - 1);

			if ((random > SPECIAL_TILE_LOWER_LIMIT) && (random < SPECIAL_TILE_UPPER_LIMIT)
					&& (tiles.get(random - 1) instanceof GreyTile) && (tiles.get(random + 1) instanceof GreyTile)) {
				Tile position = tiles.get(random);
				if (className.equals("GreenTile"))
					tiles.set(random, new GreenTile(position.getX(), position.getY(), random));
				else
					tiles.set(random, new BlackTile(position.getX(), position.getY(), random));
				counter++;
			}

		} while (counter < (int) (rate * WIDTH * HEIGHT));

	}

	public static Tile getTile(int index) throws TileIndexOutOfBoundsException {
		if (index <= (tiles.size() - 1))
			return tiles.get(index);
		else
			throw new TileIndexOutOfBoundsException("Invalid Tile Index: " + index);
	}

	public static int getStartingFuel() throws InvalidDifficultyException {
		if (difficulty == DIFFICULTY_EASY)
			return (int) (STARTING_FUEL * 1.5);
		else if (difficulty == DIFFICULTY_NORMAL)
			return STARTING_FUEL;
		else if (difficulty == DIFFICULTY_HARD)
			return STARTING_FUEL / 2;
		else
			throw new InvalidDifficultyException("Invalid game difficulty code: " + difficulty);
	}

	public ArrayList<Tile> getTiles() {
		return tiles;
	}

	@Override
	public String toString() {
		return "Grid [toString()=" + super.toString() + "]";
	}

}
