package za.ac.cput.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.cput.domain.Car;
import za.ac.cput.domain.Rental;
import za.ac.cput.domain.User;
import za.ac.cput.exception.CarNotAvailableException;
import za.ac.cput.exception.UserCantRentMoreThanOneCarException;
import za.ac.cput.factory.impl.RentalFactory;
import za.ac.cput.repository.IRentalRepository;
import za.ac.cput.service.IRentalService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("rentalServiceImpl")
public class IRentalServiceImpl implements IRentalService {
    @Autowired
    private IRentalRepository repository;

    @Autowired
    private RentalFactory rentalFactory;

/*    private IRentalServiceImpl(IRentalRepository repository) {
        this.repository = repository;
    }*/


    /*@Override
    public Rental create(Rental rental) {
        if (isCarAvailable(rental)) {
            if (isCurrentlyRenting(rental.getUser())) {
                throw new UserCantRentMoreThanOneCarException(rental.getUser().getFirstName() + " " + rental.getUser().getLastName() + " is currently renting " +  getCurrentRental(rental.getUser()).getCar().getMake() + " " + getCurrentRental(rental.getUser()).getCar().getModel() + " " + getCurrentRental(rental.getUser()).getCar().getLicensePlate());
            }
            Rental newRental = rentalFactory.create(rental);
            return repository.save(newRental);
        } else {
            throw new CarNotAvailableException(rental.getCar().getMake() +" " + rental.getCar().getModel()+ " "+rental.getCar().getLicensePlate()+", is not available for rental at this time");
        }
    }*/

    @Override
    public Rental create(Rental rental) {
        if (isCarAvailable(rental)) {
            if (isCurrentlyRenting(rental.getUser())) {
                throw new UserCantRentMoreThanOneCarException(
                        generateUserRentingErrorMessage(rental.getUser()));
            }
            Rental newRental = rentalFactory.create(rental);
            return repository.save(newRental);
        } else {
            throw new CarNotAvailableException(generateCarNotAvailableErrorMessage(rental.getCar()));
        }
    }



    @Override
    public Rental read(Integer integer) {
        return this.repository.findById(integer).orElse(null);
    }


    @Override
    public Rental read(int id) {
        return this.repository.findById(id).orElse(null);
    }

    @Override
    public Rental update(Rental rental) {
        System.out.println("IRentalServiceImpl.update : ");
        System.out.println("rental Id received : " + rental.getRentalId());

        if (repository.existsById(rental.getRentalId())) {

            System.out.println("Rental " + rental.getRentalId() + " found");
            System.out.println(rental.toString());
            Rental updatedRental = rentalFactory.create(rental);
            return repository.save(updatedRental);
        }
        System.out.println("Rental " + rental.getRentalId() + " not found");
        return null;
    }


    @Override
    public boolean delete(Integer integer) {

        if (this.repository.existsById(integer)) {
            this.repository.deleteById(integer);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        if (this.repository.existsById(id)) {
            this.repository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<Rental> getAll() {

        return (ArrayList<Rental>) this.repository.findAll();
    }
    //get all available cars

    public ArrayList<Rental> getAllAvailableCars() {
        List<Rental> allRentals = repository.findAll();
        return filterAvailableCars(allRentals);
    }
    private ArrayList<Rental> filterAvailableCars(List<Rental> rentals) {
        ArrayList<Rental> availableCars = new ArrayList<>();
        for (Rental rental : rentals) {
            if (isCarAvailable(rental)) {
                availableCars.add(rental);
            }
        }
        return availableCars;
    }
    private String generateCarNotAvailableErrorMessage(Car car) {
        return car.getMake() + " " + car.getModel() + " " +
                car.getLicensePlate() + " is not available for rental at this time";
    }
    private String generateUserRentingErrorMessage(User user) {
        Rental currentRental = getCurrentRental(user);
        Car rentedCar = currentRental.getCar();
        return user.getFirstName() +" " + user.getLastName() + " is currently renting " +
                rentedCar.getMake() + " " + rentedCar.getModel() + " " +
                rentedCar.getLicensePlate();
    }


   /* private boolean isCarAvailable(Rental rental) {
        Car carToRent = rental.getCar();
        //find rental by car id
        Optional<Rental> rentalByCarIdOrderByReturnedDateDesc = repository.findTopByCarIdOrderByReturnedDateDesc(carToRent.getId());
        //put output in a rental object if found
        Rental rentalFromDatabase = rentalByCarIdOrderByReturnedDateDesc.orElse(null);
        System.out.println("RentalFromDatabase Rental Return Date : " + rentalFromDatabase.getReturnedDate());
        System.out.println("Rental Taken Date : " + rental.getIssuedDate());

        if (rental.getReturnedDate()!=null){
            LocalDateTime timeSinceLastRental = rental.getReturnedDate().minusDays(rentalFromDatabase.getReturnedDate().getDayOfMonth());
            System.out.println("Rental Date - rentalFromDatabase : " + timeSinceLastRental);
        } else  {
            System.out.println("The car has not been returned yet");
            //time since date issue till now if car has not been returned yet(current date - date issued)
            LocalDateTime timeSinceLastRental = LocalDateTime.now().minusDays(rentalFromDatabase.getReturnedDate().getDayOfMonth());
            System.out.println("Rental Date - rentalFromDatabase : " + rental.getIssuedDate().getDayOfMonth());
        }

        //if most recent rental is returned is 24hours before new rental then car is available, if the most recent rental has not been returned or is null then car is not available
        if (rentalFromDatabase == null) { //if no rental has been made
            return true; //car is available
        } else if (rentalFromDatabase.getReturnedDate() == null) { //if most recent rental has not been returned
            return false; //car is not available
        } else if (rentalByCarIdOrderByReturnedDateDesc.get().getReturnedDate().plusDays(1).isBefore(rental.getIssuedDate())) {
            return true;
        }


        return false;

    }*/

    //refactored isCarAvailable
    private boolean isCarAvailable(Rental rental) {
        Car carToRent = rental.getCar();
        Optional<Rental> rentalFromDatabaseOptional = findMostRecentRentalByCarId(carToRent.getId());
        Rental rentalFromDatabase = rentalFromDatabaseOptional.orElse(null);

        printRentalInfo(rentalFromDatabase, rental.getReturnedDate());

        return isCarAvailableBasedOnRental(rentalFromDatabase, rental);
    }

    private Optional<Rental> findMostRecentRentalByCarId(Long carId) {
        return repository.findTopByCarIdOrderByReturnedDateDesc(Math.toIntExact(carId));
    }

    private void printRentalInfo(Rental rentalFromDatabase, LocalDateTime returnedDate) {
        if (returnedDate != null) {
            LocalDateTime timeSinceLastRental = returnedDate.minusDays(rentalFromDatabase.getReturnedDate().getDayOfMonth());
            System.out.println("Rental Date - rentalFromDatabase: " + timeSinceLastRental);
        } else {
            System.out.println("The car has not been returned yet");
            LocalDateTime timeSinceLastRental = LocalDateTime.now().minusDays(rentalFromDatabase.getIssuedDate().getDayOfMonth());
            System.out.println("Rental Date - rentalFromDatabase: " + rentalFromDatabase.getIssuedDate().getDayOfMonth());
        }
    }

    private boolean isCarAvailableBasedOnRental(Rental rentalFromDatabase, Rental rental) {
        if (rentalFromDatabase == null) {
            return true;
        } else if (rentalFromDatabase.getReturnedDate() == null) {
            return false;
        } else {
            return rentalFromDatabase.getReturnedDate().plusDays(1).isBefore(LocalDateTime.now());
        }
    }


  /*  public boolean isCarAvailableByCarId(Car car) {
        // Find rental by car id
        Optional<Rental> rentalByCarIdOrderByReturnedDateDesc = repository.findTopByCarIdOrderByReturnedDateDesc(car.getId());

        // Put output in a rental object if found
        Rental rentalFromDatabase = rentalByCarIdOrderByReturnedDateDesc.orElse(null);



        if (rentalFromDatabase != null) {
            if (rentalFromDatabase.getReturnedDate() != null) {
                LocalDateTime timeSinceLastRental = rentalFromDatabase.getReturnedDate().minusDays(rentalFromDatabase.getReturnedDate().getDayOfMonth());
                System.out.println("RentalFromDatabase Rental, Rental was returned on:  " + rentalFromDatabase.getReturnedDate());
               *//* System.out.println("Rental Date - rentalFromDatabase: " + timeSinceLastRental);*//*
                System.out.println("Car is available");
            } else {
                System.out.println("The car has not been returned yet");
                // Time since date issued till now if car has not been returned yet (current date - date issued)
                LocalDateTime timeSinceLastRental = LocalDateTime.now().minusDays(rentalFromDatabase.getIssuedDate().getDayOfMonth());
                System.out.println("Rental Date - rentalFromDatabase: " + rentalFromDatabase.getIssuedDate().getDayOfMonth());
                System.out.println("Car is not available");
            }

            // If the most recent rental is returned 24 hours before the new rental, then the car is available
            if (rentalFromDatabase.getReturnedDate().plusDays(1).isBefore(LocalDateTime.now())) {
                return true;
            }
        }

        // If no rental has been made or the most recent rental has not been returned, the car is not available
        return false;
    }*/

    //refactored isCarAvailableByCarId
    public boolean isCarAvailableByCarId(Car car) {
        Optional<Rental> rentalByCarIdOrderByReturnedDateDesc = findMostRecentRentalByCarId(car.getId());

        if (rentalByCarIdOrderByReturnedDateDesc.isPresent()) {
            Rental rentalFromDatabase = rentalByCarIdOrderByReturnedDateDesc.get();

            if (rentalFromDatabase.getReturnedDate() != null) {
                printRentalInfoForCarAvailability(rentalFromDatabase, "Car is available");

                // If the most recent rental is returned 24 hours before the new rental, then the car is available
                if (isCarAvailableBasedOnRental(rentalFromDatabase)) {
                    return true;
                }
            } else {
                printRentalInfoForCarAvailability(rentalFromDatabase, "Car is not available");
            }
        }

        return false;
    }

    private void printRentalInfoForCarAvailability(Rental rentalFromDatabase, String availabilityMessage) {
        if (rentalFromDatabase.getReturnedDate() != null) {
            LocalDateTime timeSinceLastRental = rentalFromDatabase.getReturnedDate().minusDays(rentalFromDatabase.getReturnedDate().getDayOfMonth());
            System.out.println("RentalFromDatabase Rental, Rental was returned on: " + rentalFromDatabase.getReturnedDate());
        } else {
            System.out.println("The car has not been returned yet");
            LocalDateTime timeSinceLastRental = LocalDateTime.now().minusDays(rentalFromDatabase.getIssuedDate().getDayOfMonth());
            System.out.println("Rental Date - rentalFromDatabase: " + rentalFromDatabase.getIssuedDate().getDayOfMonth());
        }
        System.out.println(availabilityMessage);
    }

    private boolean isCarAvailableBasedOnRental(Rental rentalFromDatabase) {
        return rentalFromDatabase.getReturnedDate().plusDays(1).isBefore(LocalDateTime.now());
    }



    public boolean isCurrentlyRenting(User user) {
        // Find active rentals for the user
        List<Rental> activeRentals = repository.findByUserIdAndReturnedDateIsNull(user.getId());

        // If the user has any active rentals, they are currently renting a car
        if (!activeRentals.isEmpty()) {
            return true;
        }

        return false;
    }
    public Rental getCurrentRental(User user) {
        // Find active rentals for the user
        List<Rental> activeRentals = repository.findByUserIdAndReturnedDateIsNull(user.getId());

        // If the user has any active rentals, they are currently renting a car
        if (!activeRentals.isEmpty()) {
            return activeRentals.get(0);
        }

        return null;
    }



}

