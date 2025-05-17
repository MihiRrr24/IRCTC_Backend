package ticket.booking;

import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.services.UserBookingService;
import ticket.booking.util.UserServiceeUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class App {

    private static String selectedSource, selectedDestination;

    public static void main(String[] args) {
        System.out.println("Running Train Booking System");
        Scanner scanner = new Scanner(System.in);
        int option = 0;
        UserBookingService userBookingService;
        try {
            userBookingService = new UserBookingService();
        } catch (IOException ex) {
            System.out.println("Error initializing service: " + ex.getMessage());
            return;
        }

        Train trainSelectedForBooking = null; // Initialize as null

        while (option != 7) {
            System.out.println("Choose option");
            System.out.println("1. Sign up");
            System.out.println("2. Login");
            System.out.println("3. Fetch Bookings");
            System.out.println("4. Search Trains");
            System.out.println("5. Book a Seat");
            System.out.println("6. Cancel my Booking");
            System.out.println("7. Exit the App");
            try {
                option = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (option) {
                case 1:
                    System.out.println("Enter the username to signup");
                    String nameToSignUp = scanner.nextLine();
                    System.out.println("Enter the password to signup");
                    String passwordToSignUp = scanner.nextLine();
                    User userToSignup = new User(nameToSignUp, passwordToSignUp, UserServiceeUtil.hashPassword(passwordToSignUp), new ArrayList<>(), UUID.randomUUID().toString());
                    userBookingService.signUp(userToSignup);
                    break;
                case 2:
                    System.out.println("Enter the username to Login");
                    String nameToLogin = scanner.nextLine();
                    System.out.println("Enter the password to Login");
                    String passwordToLogin = scanner.nextLine();

                    if (userBookingService.loginUser(nameToLogin, passwordToLogin)) {
                        System.out.println("Login successful! Welcome, " + nameToLogin);
                    } else {
                        System.out.println("Login failed. Invalid username or password.");
                    }
                    break;
                case 3:
                    System.out.println("Fetching your bookings");
                    userBookingService.fetchBookings();
                    break;
                case 4:
                    System.out.println("Type your source station");
                    String source = scanner.nextLine().toLowerCase();
                    System.out.println("Type your destination station");
                    String dest = scanner.nextLine().toLowerCase();
                    List<Train> trains = userBookingService.getTrains(source, dest);

                    // Check if no trains are found
                    if (trains.isEmpty()) {
                        System.out.println("No trains found between " + source + " and " + dest);
                        break;
                    }

                    // else Display available trains
                    int index = 1;
                    for (Train t : trains) {
                        System.out.println(index + ". Train id: " + t.getTrainId());
                        for (Map.Entry<String, String> entry : t.getStationTimes().entrySet()) {
                            System.out.println("  Station: " + entry.getKey() + ", Time: " + entry.getValue());
                        }
                        index++;
                    }

                    // Prompt user to select a train
                    System.out.println("Select a train by typing 1, 2, 3...");
                    try {
                        int userInput = Integer.parseInt(scanner.nextLine());
                        int selectedIndex = userInput - 1;
                        if (selectedIndex >= 0 && selectedIndex < trains.size()) {
                            trainSelectedForBooking = trains.get(selectedIndex);
                            selectedSource = source;
                            selectedDestination = dest;
                            System.out.println("Selected train: " + trainSelectedForBooking.getTrainId());
                        } else {
                            System.out.println("Invalid selection. Please choose a number between 1 and " + trains.size());
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid number.");
                    }
                    break;
                case 5:
                    if (trainSelectedForBooking == null) {
                        System.out.println("Please search and select a train first (Option 4)");
                        break;
                    }
                    System.out.println("Select a seat out of these seats");
                    List<List<Integer>> seats = userBookingService.fetchSeats(trainSelectedForBooking);
                    for (List<Integer> row : seats) {
                        for (Integer val : row) {
                            System.out.print(val + " ");
                        }
                        System.out.println();
                    }
                    System.out.println("Select the seat by typing the row and column");
                    System.out.println("Enter the row");
                    try {
                        String row = scanner.nextLine();
                        System.out.println("Enter the column");
                        String col = scanner.nextLine();
                        System.out.println("Enter the date of travel (YYYY-MM-DD):");
                        String dateOfTravel = scanner.nextLine();
                        try {
                            java.time.LocalDate parsedDate = java.time.LocalDate.parse(dateOfTravel, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                            if (parsedDate.isBefore(java.time.LocalDate.now())) {
                                System.out.println("Date must be today or in the future.");
                                System.out.println("Enter the date of travel (YYYY-MM-DD):");
                                dateOfTravel = scanner.nextLine();
                            }
                            System.out.println("Booking your seat....");
                            boolean booked = userBookingService.bookTicket(trainSelectedForBooking, row, col, selectedSource, selectedDestination, dateOfTravel);
                            if (booked) {
                                System.out.println("Booked! Enjoy your journey");
                            } else {
                                System.out.println("Can't book this seat");
                            }
                        } catch (java.time.format.DateTimeParseException e) {
                            System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter valid numbers for row and column.");
                    }
                    break;
                case 6:
                    // Check if user has logged in
                    if(!userBookingService.isLoggedIn()){
                        System.out.println("Please Log In First");
                        break;
                    }
                    // Check if user has any bookings
                    List<Ticket> tickets = userBookingService.getUserTickets();
                    if (tickets.isEmpty()) {
                        System.out.println("No bookings found. Please book a ticket first.");
                        break;
                    }

                    // Display user's bookings
                    System.out.println("Your bookings:");
                    for (int i = 0; i < tickets.size(); i++) {
                        Ticket ticket = tickets.get(i);
                        System.out.println((i + 1) + ". Ticket ID: " + ticket.getTicketId());
                        System.out.println("   Train ID: " + ticket.getTrain().getTrainId());
                        System.out.println("   Source: " + ticket.getSource() + ", Destination: " + ticket.getDestination());
                        System.out.println("   Date of Travel: " + ticket.getDateOfTravel());
                        System.out.println("   Seat: Row " + ticket.getRow() + ", Col " + ticket.getCol());
                    }

                    // Prompt user to select a ticket to cancel
                    System.out.println("Select a ticket to cancel by typing 1, 2, 3...");
                    try {
                        int userInput = Integer.parseInt(scanner.nextLine());
                        int selectedIndex = userInput - 1;
                        if (selectedIndex >= 0 && selectedIndex < tickets.size()) {
                            Ticket ticketToCancel = tickets.get(selectedIndex);
                            Boolean cancelled = userBookingService.cancelBooking(ticketToCancel.getTicketId());
                            if (cancelled) {
                                System.out.println("Ticket cancelled successfully.");
                            } else {
                                System.out.println("Failed to cancel ticket.");
                            }
                        } else {
                            System.out.println("Invalid selection. Please choose a number between 1 and " + tickets.size());
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid number.");
                    }
                    break;
                case 7:
                    System.out.println("Exiting the Train Booking System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please choose a number between 1 and 7.");
                    break;
            }
        }
        scanner.close(); // Close scanner to prevent resource leak
    }
}
