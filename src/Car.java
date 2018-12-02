import java.util.Random;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Car {

	private int fuel;
	private boolean isRed;
	private Tile position;
	public String name;
	public boolean isRefilling;
	public int turnsRefilling;
	private short lastCast;
	private ImageView image;
	// Out of Fuel Window
	private Stage noFuelWindow;
	private ComboBox<String> noFuelCB;
	private Label noFuelLB;
	private Label turnsStandByLB;
	private ComboBox<String> turnsStandByCB;
	private Button noFuelSubmitBtn;

	private EventHandler<ActionEvent> onNoFuelOptionSelectedHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent arg0) {
			if (noFuelCB.getSelectionModel().getSelectedIndex() == 0) {
				turnsStandByCB.setDisable(true);
			} else if (noFuelCB.getSelectionModel().getSelectedIndex() == 1) {
				turnsStandByCB.setDisable(false);
			}
		}
	};

	private EventHandler<ActionEvent> onNoFuelSubmitHandler = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			Button source = (Button) (event.getSource());
			Car car = (Car) source.getUserData();

			if (noFuelCB.getSelectionModel().getSelectedIndex() == 0
					|| (noFuelCB.getSelectionModel().getSelectedIndex() == 1
							&& turnsStandByCB.getSelectionModel().getSelectedIndex() > 0)) {
				if (noFuelCB.getSelectionModel().getSelectedIndex() == 0) {
					try {
						car.returnToStart();
					} catch (TileIndexOutOfBoundsException e) {
						RaceGame.exitWithError(e.getMessage());
						return;
					}
					car.addFuel(RaceGame.getRandomNumber(120));
					RaceGame.printlnToConsole(car.getDisplayName() + ", returned to the start.");
				} else if (noFuelCB.getSelectionModel().getSelectedIndex() == 1
						&& turnsStandByCB.getSelectionModel().getSelectedIndex() > 0) {
					car.isRefilling = true;
					car.turnsRefilling = turnsStandByCB.getSelectionModel().getSelectedIndex() + 1;
					RaceGame.printlnToConsole(car.getDisplayName() + ", will stand by the next " + car.turnsRefilling
							+ " turns to get +20 litres of Fuel (each)!");
				}
				noFuelWindow.close();
			}
		}
	};

	public Car(int fuel, Tile position, boolean isRed, String name, ImageView image) {
		this.fuel = fuel;
		this.isRed = isRed;
		this.position = position;
		this.name = name;
		this.image = image;

		noFuelWindow = new Stage();
		noFuelWindow.initModality(Modality.APPLICATION_MODAL);
		noFuelWindow.setWidth(500);
		noFuelWindow.setHeight(200);
		noFuelWindow.setResizable(false);
		noFuelLB = new Label("Options:");
		noFuelLB.setTranslateX(25);
		noFuelLB.setTranslateY(25);
		noFuelCB = new ComboBox<String>();
		noFuelCB.getItems().addAll("Return to start and refill a random amount of fuel (1 - 120)",
				"Stop for 1-6 turns, and get +20 fuel (each).");
		noFuelCB.setTranslateX(125);
		noFuelCB.setTranslateY(25);
		turnsStandByLB = new Label("Turns to Stand By");
		turnsStandByLB.setTranslateX(25);
		turnsStandByLB.setTranslateY(75);
		turnsStandByCB = new ComboBox<String>();
		turnsStandByCB.getItems().addAll("1", "2", "3", "4", "5", "6");
		turnsStandByCB.setTranslateX(125);
		turnsStandByCB.setTranslateY(75);
		turnsStandByCB.setDisable(true);
		noFuelSubmitBtn = new Button("Continue");
		noFuelSubmitBtn.setTranslateX(175);
		noFuelSubmitBtn.setTranslateY(125);
	}

	public int castDie() {
		short value = (short) (new Random().nextInt(6) + 1);
		lastCast = value;
		return lastCast;
	}

	public void move(Tile target) throws CarOutOfFuelException, SteppedIntoBlackTileException,
			TileIndexOutOfBoundsException, CarWonException {

		if (target.getGridIndex() > Grid.SIZE)
			target = Grid.getTile(Grid.SIZE);

		RaceGame.printlnToConsole(getDisplayName() + " steps into:");

		for (int i = position.getGridIndex() + 1; i <= target.getGridIndex(); i++) {

			moveImage(Grid.getTile(i));
			position = Grid.getTile(i);

			if (Grid.getTile(i) instanceof GreenTile) {
				RaceGame.printlnToConsole("a Green Tile!");
				addFuel((int) (fuel * 0.5));
			} else if (i == target.getGridIndex() && Grid.getTile(i) instanceof BlackTile) {
				RaceGame.printlnToConsole("a Black Tile!");
				refreshFuelLabel();
				throw new SteppedIntoBlackTileException(name);
			} else if (Grid.getTile(i) instanceof GreyTile) {
				int fuelCost = ((GreyTile) Grid.getTile(i)).getFuelCost();
				RaceGame.printlnToConsole("a Grey Tile (-" + fuelCost + " fuel).");
				fuel -= fuelCost;
			}

			if (fuel <= 0) {
				fuel = 0;
				refreshFuelLabel();
				throw new CarOutOfFuelException(name);
			}

			if (position.getGridIndex() >= Grid.SIZE) {
				refreshFuelLabel();
				throw new CarWonException(name);
			}
		}

		refreshFuelLabel();
		RaceGame.printlnToConsole(getDisplayName() + ", moved into: " + position.getCode());

		RaceGame.printlnToConsole("------------------");
		RaceGame.printlnToConsole("------------------");
	}

	public void moveImage(Tile target) throws TileIndexOutOfBoundsException {
		if (target.getGridIndex() == 0) {
			image.setTranslateX(RaceGame.CANVAS_X);
			image.setTranslateY(RaceGame.CANVAS_Y + (isRed ? 0 : RaceGame.TILE_HEIGHT / 2));
		} else if (target.getY() > Grid.getTile(target.getGridIndex() - 1).getY()) { // Player moved into different row
			image.setTranslateX(RaceGame.CANVAS_X + target.getX() * RaceGame.TILE_WIDTH);
			if (isRed)
				image.setTranslateY(RaceGame.CANVAS_Y + target.getY() * RaceGame.TILE_HEIGHT);
			else
				image.setTranslateY(
						RaceGame.CANVAS_Y + target.getY() * RaceGame.TILE_HEIGHT + RaceGame.TILE_HEIGHT / 2);
		} else { // Player moved into same row
			image.setTranslateX(RaceGame.CANVAS_X + target.getX() * RaceGame.TILE_WIDTH);
		}
	}

	public String getDisplayName() {
		return name + " " + (isRed ? "(Red Car)" : "(Blue Car)");
	}

	public void addFuel(int amount) {
		fuel += amount;
		RaceGame.printlnToConsole(getDisplayName() + ", refilled " + amount + " fuel.");
		refreshFuelLabel();
	}

	public void refreshFuelLabel() {
		if (name.equals(RaceGame.player1.name))
			RaceGame.player1Fuel.setText(String.valueOf(fuel));
		else
			RaceGame.player2Fuel.setText(String.valueOf(fuel));
	}

	public void returnToStart() throws TileIndexOutOfBoundsException {
		position = Grid.getTile(0);
		moveImage(position);
		RaceGame.printlnToConsole(getDisplayName() + ", moved into: " + getPosition().getCode());
	}

	public void showNoFuelWindow() {
		noFuelCB.setOnAction(onNoFuelOptionSelectedHandler);

		noFuelSubmitBtn.setUserData(this);
		noFuelSubmitBtn.setOnAction(onNoFuelSubmitHandler);

		Group root = new Group();
		root.getChildren().addAll(noFuelSubmitBtn, turnsStandByCB, noFuelCB, turnsStandByLB, noFuelLB);
		noFuelWindow.setTitle(getDisplayName() + ", please select one of the following options");
		noFuelWindow.setScene(new Scene(root, 500, 500));
		noFuelWindow.showAndWait();
	}

	public String getUsername() {
		return name;
	}

	public boolean isRed() {
		return isRed;
	}

	public int getFuel() {
		return fuel;
	}

	public Tile getPosition() {
		return position;
	}

	public int getLastCast() {
		return lastCast;
	}

	public void setLastCast(short lastCast) {
		this.lastCast = lastCast;
	}

	public ImageView getImage() {
		return image;
	}

	public void setImage(ImageView image) {
		this.image = image;
	}

	@Override
	public String toString() {
		return "Car [fuel=" + fuel + ", name=" + name + ", position=" + position + ", toString()=" + super.toString()
				+ "]";
	}

}
