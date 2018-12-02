import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/* 
 * Finish Flag Image: https://www.flaticon.com/free-icon/racing_512512#term=finish%20flag&page=1&position=25
 */
public class RaceGame extends Application {

	public static final String VERSION = "1.0.0";
	public static final double BLACK_TILE_RATE = 0.05;
	public static final double GREEN_TILE_RATE = 0.04;
	private final String IMAGES_PATH = System.getProperty("user.dir") + "\\images\\";

	public static Grid grid;
	public static Car player1;
	public static Car player2;
	private boolean isPlayer1Turn;
	private boolean onStartingPhase = true;
	private ArrayList<Label> gridCodes;
	private static Stage primaryStage;

	// Starting Window
	private Group startingWindow;
	private ComboBox<String> difficultyCB;
	private Label difficultyLB;
	private Button startSubmitBtn;

	// Car Select Window
	private Group carSelectWindow;
	private ComboBox<String> player1CarDropdown;
	private TextField username1TF;
	private TextField username2TF;
	private Label username1LB;
	private Label username2LB;
	private Label car1Label;
	private Button playBtn;

	// Play Window
	private Group playWindow;
	public static Label player1Fuel;
	public static Label player2Fuel;
	private Label player1LB;
	private static TextArea console;
	private Button castDieBtn;
	// Grid (Canvas)
	public static final int TILE_WIDTH = 50;
	public static final int TILE_HEIGHT = 50;
	public static final int CANVAS_X = 125;
	public static final int CANVAS_Y = 200;
	private byte greyImageData[] = new byte[TILE_WIDTH * TILE_HEIGHT * 3];
	private byte blackImageData[] = new byte[TILE_WIDTH * TILE_HEIGHT * 3];
	private byte greenImageData[] = new byte[TILE_WIDTH * TILE_HEIGHT * 3];
	private ImageView blueCarImg;
	private ImageView redCarImg;
	private ImageView finishFlag;
	private GraphicsContext gc;
	private Canvas canvas;

	// Event Handlers
	private EventHandler<ActionEvent> onCastClickHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			Car player = (isPlayer1Turn ? player1 : player2);

			if (onStartingPhase) {
				player.castDie();
				printlnToConsole(player.getDisplayName() + ", casted " + player.getLastCast());
				if (player1.getLastCast() > 0 && player2.getLastCast() > 0) {
					if (player1.getLastCast() > player2.getLastCast()) {
						printlnToConsole(player1.getDisplayName() + ", plays first.");
						onStartingPhase = false;
						isPlayer1Turn = false;
					} else if (player2.getLastCast() > player1.getLastCast()) {
						printlnToConsole(player2.getDisplayName() + ", plays first.");
						onStartingPhase = false;
						isPlayer1Turn = true;
					} else {
						player1.setLastCast((short) 0);
						player2.setLastCast((short) 0);
						printlnToConsole("Result is a TIE. Please repeat the process");
					}
				}
			} else {
				if (!player.isRefilling) {
					printlnToConsole("**********");
					player.castDie();

					printlnToConsole(player.getDisplayName() + ", casted " + player.getLastCast());

					try {
						if (player.getLastCast() + player.getPosition().getGridIndex() > Grid.SIZE)
							player.move(Grid.getTile(Grid.SIZE));
						else
							player.move(player.getPosition().getNextTileAt(player.getLastCast()));
					} catch (CarOutOfFuelException e) {
						printlnToConsole(player.getDisplayName() + ", moved into: " + player.getPosition().getCode());
						printlnToConsole(player.getDisplayName() + ", ran out of fuel!");
						player.showNoFuelWindow();
					} catch (SteppedIntoBlackTileException e) {
						printlnToConsole(player.getDisplayName() + ", moved into: " + player.getPosition().getCode());
						try {
							player.returnToStart();
						} catch (TileIndexOutOfBoundsException e1) {
							exitWithError(e1.getMessage());
						}
						printlnToConsole(player.getDisplayName() + ", returned to the start!");
					} catch (TileIndexOutOfBoundsException e) {
						exitWithError(e.getMessage());
						return;
					} catch (CarWonException e) {
						printlnToConsole(player.getDisplayName() + ", moved into: " + player.getPosition().getCode());
						displayWinner(player);
						return;
					}
				} else {
					if (player.turnsRefilling == 1) {
						player.isRefilling = false;
						player.turnsRefilling = 0;
					} else {
						player.turnsRefilling--;
					}
					player.addFuel(20);
				}
			}

			switchTurn();
		}
	};

	private EventHandler<ActionEvent> onStartSubmitHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent e) {
			int selection = difficultyCB.getSelectionModel().getSelectedIndex();
			if (selection > -1 && selection <= 2) {
				setGrid(difficultyCB.getSelectionModel().getSelectedIndex() + 1);
				showCarSelectWindow();
			}
		}
	};

	private EventHandler<ActionEvent> onPlayClickHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			if ((username1TF.getText().length() > 0) && (username2TF.getText().length() > 0)
					&& (player1CarDropdown.getSelectionModel().getSelectedIndex() > -1)) {
				try {
					assignCars(player1CarDropdown.getSelectionModel().getSelectedIndex() == 0, username1TF.getText(),
							username2TF.getText());
				} catch (TileIndexOutOfBoundsException exception) {
					exitWithError(exception.getMessage());
					event.consume();
					return;
				}
				showPlayWindow();
			}
		}
	};

	public static void main(String[] args) {
		launch(args);
	}

	public RaceGame() {
		gridCodes = new ArrayList<Label>(Grid.WIDTH + Grid.HEIGHT);

		// Starting Window
		startingWindow = new Group();
		// Player 1 Username
		username1LB = new Label("Username 1:");
		username1LB.setTranslateX(100);
		username1LB.setTranslateY(20);
		username1TF = new TextField("Joe");
		username1TF.setTranslateX(175);
		username1TF.setTranslateY(20);
		// Game Difficulty
		difficultyCB = new ComboBox<String>();
		difficultyCB.getItems().addAll("Easy (Double fuel, + Green T., - Black T.)", "Normal",
				"Hard (Half fuel, - Green T.)");
		difficultyLB = new Label("Difficulty");
		difficultyCB.setTranslateX(125);
		difficultyCB.setTranslateY(20);
		difficultyLB.setTranslateX(50);
		difficultyLB.setTranslateY(20);
		// Player 2 Username
		username2LB = new Label("Username 2:");
		username2LB.setTranslateX(100);
		username2LB.setTranslateY(60);
		username2TF = new TextField("Doe");
		username2TF.setTranslateX(175);
		username2TF.setTranslateY(60);
		// Start Submit Button
		startSubmitBtn = new Button("Continue");
		startSubmitBtn.setTranslateX(185);
		startSubmitBtn.setTranslateY(70);

		// Car Select Window
		carSelectWindow = new Group();
		// Player 1 Car Color
		car1Label = new Label("Player 1 Car:");
		car1Label.setTranslateX(100);
		car1Label.setTranslateY(100);
		player1CarDropdown = new ComboBox<String>();
		player1CarDropdown.getItems().addAll("Red", "Blue");
		player1CarDropdown.setTranslateX(175);
		player1CarDropdown.setTranslateY(100);
		// Play Button
		playBtn = new Button("Play");
		playBtn.setTranslateX(175);
		playBtn.setTranslateY(140);

		// Play Window
		playWindow = new Group();
		// Console
		console = new TextArea();
		console.setFont(new Font(20.0));
		console.setMaxWidth(500);
		console.setMaxHeight(125);
		console.setWrapText(true);
		console.setTranslateX(CANVAS_X + 25);
		console.setTranslateY(10);
		castDieBtn = new Button("Cast Die");
		castDieBtn.setScaleX(1.3);
		castDieBtn.setScaleY(1.3);
		castDieBtn.setTranslateX(CANVAS_X + 200);
		castDieBtn.setTranslateY(150);
		canvas = new Canvas(Grid.WIDTH * TILE_WIDTH, Grid.HEIGHT * TILE_HEIGHT);
		canvas.setTranslateX(CANVAS_X);
		canvas.setTranslateY(CANVAS_Y);
		gc = canvas.getGraphicsContext2D();

		try {
			blueCarImg = getImage(IMAGES_PATH + "blue-car.jpg", CANVAS_X, CANVAS_Y + TILE_HEIGHT / 2, TILE_WIDTH,
					TILE_HEIGHT / 2);
			redCarImg = getImage(IMAGES_PATH + "red-car.jpg", CANVAS_X, CANVAS_Y, TILE_WIDTH, TILE_HEIGHT / 2);
			finishFlag = getImage(IMAGES_PATH + "finish-flag.png", CANVAS_X + TILE_WIDTH * 9,
					CANVAS_Y + TILE_HEIGHT * 9, TILE_WIDTH, TILE_HEIGHT);
		} catch (NullPointerException e) {
			e.printStackTrace();
			exitWithError(e.getMessage());
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		RaceGame.primaryStage = primaryStage;
		showStartingWindow();
	}

	private void showStartingWindow() {
		// Stage
		primaryStage.setTitle("Select game difficulty");

		startSubmitBtn.setOnAction(onStartSubmitHandler);

		startingWindow.getChildren().addAll(difficultyCB, difficultyLB, startSubmitBtn);

		primaryStage.setScene(new Scene(startingWindow, 400, 100));
		primaryStage.setResizable(false);
		primaryStage.sizeToScene();
		primaryStage.show();
	}

	private void showCarSelectWindow() {
		primaryStage.setTitle("Configure Settings");

		playBtn.setOnAction(onPlayClickHandler);

		carSelectWindow.getChildren().addAll(username1LB, username2LB, username1TF, username2TF, car1Label,
				player1CarDropdown, playBtn);
		primaryStage.setScene(new Scene(carSelectWindow, 400, 250));
	}

	private void showPlayWindow() {
		primaryStage.setTitle("Race Game " + VERSION);

		player1LB = new Label(player1.name);
		player1LB.setScaleX(2);
		player1LB.setScaleY(2);
		player1LB.setTranslateX(25);
		player1LB.setTranslateY(20);
		player1LB.setTextFill(player1.isRed() ? Color.web("FF0000") : Color.web("0000FF"));

		player1Fuel = new Label(String.valueOf(player1.getFuel()));
		player1Fuel.setScaleX(2);
		player1Fuel.setScaleY(2);
		player1Fuel.setTranslateX(25);
		player1Fuel.setTranslateY(60);

		castDieBtn.setOnAction(onCastClickHandler);

		Label player2Label = new Label(player2.name);
		player2Label.setTextFill(player2.isRed() ? Color.web("FF0000") : Color.web("0000FF"));
		player2Label.setScaleX(2);
		player2Label.setScaleY(2);
		player2Label.setTranslateX(CANVAS_X + TILE_WIDTH * 10 + 50);
		player2Label.setTranslateY(20);

		player2Fuel = new Label(String.valueOf(player1.getFuel()));
		player2Fuel.setScaleX(2);
		player2Fuel.setScaleY(2);
		player2Fuel.setTranslateX(CANVAS_X + TILE_WIDTH * 10 + 50);
		player2Fuel.setTranslateY(60);

		createTileData();
		drawTileData();

		playWindow.getChildren().addAll(console, canvas, redCarImg, blueCarImg, player1LB, player2Label, castDieBtn,
				player1Fuel, player2Fuel, finishFlag);
		playWindow.getChildren().addAll(gridCodes);
		primaryStage.setScene(new Scene(playWindow, 750, 750));

		getStartingTurn();
	}

	public void switchTurn() {
		isPlayer1Turn = !isPlayer1Turn;

		if (isPlayer1Turn)
			printlnToConsole(player1.getDisplayName() + ", please cast the die.");
		else
			printlnToConsole(player2.getDisplayName() + ", please cast the die.");
	}

	public void displayWinner(Car player) {
		printlnToConsole("---------------------");
		printlnToConsole("Congratulations!");
		printlnToConsole(player.getDisplayName() + " HAS WON!");
		printlnToConsole("");
		castDieBtn.setDisable(true);
		if (player1.equals(player))
			displayStatistics(player1, player2);
		else
			displayStatistics(player2, player1);
	}

	private void displayStatistics(Car winner, Car loser) {
		printlnToConsole("");
		printlnToConsole("--------STATISTICS--------");
		printlnToConsole(winner.getDisplayName() + " has scored: " + winner.getFuel() + " points");
		printlnToConsole(loser.getDisplayName() + " was in position: " + loser.getPosition().getCode());
	}

	public static int getRandomNumber(int max) {
		return new Random().nextInt(max) + 1;
	}

	public ImageView getImage(String src, int x, int y, int width, int height) throws NullPointerException {
		File file = new File(src);
		Image image = new Image(file.toURI().toString());
		ImageView imageNode = new ImageView(image);
		imageNode.setTranslateX(x);
		imageNode.setTranslateY(y);
		imageNode.setFitWidth(width);
		imageNode.setFitHeight(height);
		return imageNode;
	}

	private void createTileData() {
		int i = 0;
		for (int y = 0; y < TILE_HEIGHT; y++) {
			for (int x = 0; x < TILE_WIDTH; x++) {
				if ((x == 0) || (x == TILE_WIDTH - 1) || (y == 0) || (y == TILE_HEIGHT - 1)) {
					greyImageData[i] = greyImageData[i
							+ 1] = greyImageData[i + 2] = blackImageData[i] = blackImageData[i + 1] = blackImageData[i
									+ 2] = greenImageData[i] = greenImageData[i + 1] = greenImageData[i + 2] = (byte) 0;
				} else {
					greyImageData[i] = greyImageData[i + 1] = greyImageData[i + 2] = (byte) 185;
					blackImageData[i] = blackImageData[i
							+ 1] = blackImageData[i + 2] = greenImageData[i] = greenImageData[i + 2] = 0;
					greenImageData[i + 1] = (byte) 255;
				}
				i += 3;
			}
		}
	}

	private void drawTileData() {
		PixelWriter pixelWriter = gc.getPixelWriter();
		PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();

		for (int y = 0; y < 500; y += TILE_HEIGHT) {
			for (int x = 0; x < 500; x += TILE_WIDTH) {
				Tile position;
				try {
					position = Grid.getTile((y / TILE_HEIGHT) * 10 + x / TILE_WIDTH);
				} catch (TileIndexOutOfBoundsException e) {
					exitWithError(e.getMessage());
					return;
				}

				if (position.getGridIndex() == 0) { // first tile
					Label codeX = new Label(String.valueOf(position.getX() + 1));
					codeX.setUserData(position.getGridIndex());
					codeX.setTranslateX(CANVAS_X - 25);
					codeX.setTranslateY(CANVAS_Y + position.getY() * TILE_HEIGHT + TILE_HEIGHT / 2);
					Label codeY = new Label(Grid.alphabet[position.getY()]);
					codeY.setUserData(position.getGridIndex());
					codeY.setTranslateX(CANVAS_X + position.getX() * TILE_WIDTH + TILE_WIDTH / 2);
					codeY.setTranslateY(CANVAS_Y - 25);
					gridCodes.add(codeX);
					gridCodes.add(codeY);
				} else if (position.getY() == 0 && position.getX() > 0) { // first row (not first tile)
					Label codeX = new Label(Grid.alphabet[position.getX()]);
					codeX.setUserData(position.getGridIndex());
					codeX.setTranslateX(CANVAS_X + position.getX() * TILE_WIDTH + TILE_WIDTH / 2);
					codeX.setTranslateY(CANVAS_Y - 25);
					gridCodes.add(codeX);
				} else if (position.getX() == 0 && position.getY() > 0) { // first tile of n row (n > 1)
					Label codeY = new Label(String.valueOf(position.getY() + 1));
					codeY.setUserData(position.getGridIndex());
					codeY.setTranslateX(CANVAS_X - 25);
					codeY.setTranslateY(CANVAS_Y + position.getY() * TILE_HEIGHT + TILE_HEIGHT / 2);
					gridCodes.add(codeY);
				}

				if (position instanceof GreyTile)
					pixelWriter.setPixels(x, y, TILE_WIDTH, TILE_HEIGHT, pixelFormat, greyImageData, 0, TILE_WIDTH * 3);
				else if (position instanceof GreenTile)
					pixelWriter.setPixels(x, y, TILE_WIDTH, TILE_HEIGHT, pixelFormat, greenImageData, 0,
							TILE_WIDTH * 3);
				else
					pixelWriter.setPixels(x, y, TILE_WIDTH, TILE_HEIGHT, pixelFormat, blackImageData, 0,
							TILE_WIDTH * 3);
			}
		}
	}

	public static void exitWithError(String message) {
		if (primaryStage instanceof Stage) // if primary stage is open
			primaryStage.close();

		Stage errorWindow = new Stage();
		errorWindow.initModality(Modality.APPLICATION_MODAL);
		errorWindow.setWidth(200);
		errorWindow.setHeight(100);
		errorWindow.setResizable(false);
		Label errorLB = new Label("Error: " + message);
		errorLB.setWrapText(true);
		errorLB.setMaxWidth(125);
		errorLB.setMaxHeight(100);
		errorLB.setMinWidth(100);
		errorLB.setMinHeight(50);
		errorLB.setTranslateX(50);
		errorLB.setTranslateY(15);

		Group root = new Group();
		root.getChildren().add(errorLB);
		errorWindow.setTitle("Fatal Error");
		errorWindow.setScene(new Scene(root, 500, 500));
		errorWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				e.consume();
				System.exit(0);
			}
		});
		errorWindow.showAndWait();
	}

	public ImageView getBlueCarImg() {
		return blueCarImg;
	}

	public ImageView getRedCarImg() {
		return redCarImg;
	}
	
	private void setGrid(int difficulty) {
		try {
			grid = new Grid(difficulty);
		} catch (InvalidTileCoordException e) {
			e.printStackTrace();
			exitWithError(e.getMessage());
		} catch (InvalidDifficultyException e) {
			e.printStackTrace();
			exitWithError(e.getMessage());
		}
	}

	public static void printlnToConsole(String text) {
		console.setText(console.getText() + text + "\n");
		console.setScrollTop(Double.MAX_VALUE);
	}

	public static void printToConsole(String text) {
		console.setText(console.getText() + text);
	}

	@SuppressWarnings("static-access")
	private void assignCars(boolean player1Red, String username1, String username2)
			throws TileIndexOutOfBoundsException {
		try {
			if (player1Red) {
				player1 = new Car(grid.getStartingFuel(), Grid.getTile(0), true, username1, redCarImg);
				player2 = new Car(grid.getStartingFuel(), Grid.getTile(0), !player1Red, username2, blueCarImg);
			} else {
				player1 = new Car(grid.getStartingFuel(), Grid.getTile(0), true, username1, blueCarImg);
				player2 = new Car(grid.getStartingFuel(), Grid.getTile(0), !player1Red, username2, redCarImg);
			}
		} catch (InvalidDifficultyException e) {
			exitWithError(e.getMessage());
		}
	}

	public void getStartingTurn() {
		printlnToConsole("\nBoth players will cast a die, to find out who will play first.");
		printlnToConsole("-----------------");

		isPlayer1Turn = true;
		printlnToConsole(player1.getDisplayName() + ", please cast the die.");
	}

}