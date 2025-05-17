package ticket.booking.services;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.util.UserServiceeUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UserBookingService {

    private ObjectMapper objectMapper = new ObjectMapper();

    private List<User> userList;

    private User user;

    private final String USER_FILE_PATH = "app/src/main/java/ticket/booking/localDb/users.json";

    public UserBookingService() throws IOException {
        loadUserListFromFile();
    }

    private void loadUserListFromFile() throws IOException {
        userList = objectMapper.readValue(new File(USER_FILE_PATH), new TypeReference<List<User>>() {});
    }

    public boolean loginUser(String username, String password) {
        Optional<User> foundUser = userList.stream()
                .filter(user1 -> user1.getName().equals(username) &&
                        UserServiceeUtil.checkPassword(password, user1.getHashedPassword()))
                .findFirst();
        if (foundUser.isPresent()) {
            this.user = foundUser.get(); // Set the logged-in user
            return true;
        }
        return false;
    }

    public Boolean signUp(User user1){
        try{
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        }catch (IOException ex){
            return Boolean.FALSE;
        }
    }

    public boolean isLoggedIn(){
        return (user != null);
    }

    private void saveUserListToFile() throws IOException {
        File usersFile = new File(USER_FILE_PATH);
        objectMapper.writeValue(usersFile, userList);
    }

    public void fetchBookings(){
        Optional<User> userFetched = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceeUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        if(userFetched.isPresent()){
            userFetched.get().printTickets();
        } else{
            System.out.println("User not found or Invalid Credentials");
        }
    }

    public List<Ticket> getUserTickets() {
        Optional<User> userFetched = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceeUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        if (userFetched.isPresent()) {
            return userFetched.get().getTicketsBooked();
        }
        return new ArrayList<>();
    }

    public Boolean cancelBooking(String ticketId) {
        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return Boolean.FALSE;
        }

        Optional<Ticket> ticketToCancel = user.getTicketsBooked().stream()
                .filter(ticket -> ticket.getTicketId().equals(ticketId))
                .findFirst();

        if (ticketToCancel.isPresent()) {
            try {
                Ticket ticket = ticketToCancel.get();
                Train train = ticket.getTrain();

                // Get the seat position (assumes Ticket class has row and col fields)
                int row = Integer.parseInt(ticket.getRow());
                int col = Integer.parseInt(ticket.getCol());

                // Free the seat
                List<List<Integer>> seats = train.getSeats();
                if (row >= 0 && row < seats.size() && col >= 0 && col < seats.get(row).size()) {
                    seats.get(row).set(col, 0);
                    train.setSeats(seats);

                    // Update train data
                    TrainService trainService = new TrainService();
                    trainService.addTrain(train);

                    // Remove ticket from user's bookings
                    user.getTicketsBooked().remove(ticket);
                    saveUserListToFile();

                    System.out.println("Ticket with ID " + ticketId + " has been canceled.");
                    return Boolean.TRUE;
                } else {
                    System.out.println("Invalid seat position for ticket ID " + ticketId);
                    return Boolean.FALSE;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error saving changes after cancellation.");
                return Boolean.FALSE;
            }
        } else {
            System.out.println("No ticket found with ID " + ticketId);
            return Boolean.FALSE;
        }
    }


    public List<Train> getTrains(String source, String destination){
        try{
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        }catch(IOException ex){
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train){
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, String rowStr, String colStr) {
        try{
            int row = Integer.parseInt(rowStr);
            int seat = Integer.parseInt(colStr);

            TrainService trainService = new TrainService();
            List<List<Integer>> seats = train.getSeats();
            if (row >= 0 && row < seats.size() && seat >= 0 && seat < seats.get(row).size()) {
                if (seats.get(row).get(seat) == 0) {
                    seats.get(row).set(seat, 1);
                    train.setSeats(seats);
                    trainService.addTrain(train);
                    return true; // Booking successful
                } else {
                    return false; // Seat is already booked
                }
            } else {
                return false; // Invalid row or seat index
            }
        }catch (IOException ex){
            return Boolean.FALSE;
        }
    }

    public boolean bookTicket(Train train, String row, String seat, String source, String destination, String dateOfTravel) {
        try {
            if (bookTrainSeat(train, row, seat)) {
                String ticketId = UUID.randomUUID().toString();
                String userId = this.user.getUserId();
                Ticket ticket = new Ticket(ticketId, userId, source, destination, dateOfTravel, train, row, seat);
                this.user.getTicketsBooked().add(ticket);
                saveUserListToFile();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
