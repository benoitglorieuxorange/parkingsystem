package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private String vehicleRegNumber;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @Mock
    private FareCalculatorService fareCalculatorService;

    @InjectMocks
    private ParkingService parkingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.setFareCalculatorService(fareCalculatorService);

    }


    @Test
    public void processExitingVehicleTest() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setVehicleRegNumber("BG-123-BG");

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        when(ticketDAO.getTicket(vehicleRegNumber)).thenReturn(ticket);
        when(ticketDAO.getNBTicket(vehicleRegNumber)).thenReturn(2);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).getNBTicket(vehicleRegNumber);
        verify(ticketDAO).getTicket(vehicleRegNumber);
        verify(ticketDAO).getNBTicket(vehicleRegNumber);
        verify(ticketDAO).updateTicket(any(Ticket.class));

        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
        assertNotNull(ticket.getOutTime());


    }


    @Test
    public void testProcessExitingVehicle_ShouldCallCalculateFare() throws Exception {
        // Arrange
        String regNumber = "BG-123-BG";
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber(regNumber);
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));


        when(parkingService.getVehichleRegNumber()).thenReturn("BG-123-BG");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNBTicket(regNumber)).thenReturn(2);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        doNothing().when(fareCalculatorService).calculateFare(any(Ticket.class), anyBoolean());

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(fareCalculatorService, atLeastOnce()).calculateFare(any(Ticket.class), anyBoolean());


    }


    // Test 1
    @Test
    public void testProcessIncomingVehicle() throws Exception {

        //type de véhicule 1 CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        //Emplacement 1 disponible pour une voiture
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        //L'utilisateur à saisi BG-123-BG comme numéro de plaque
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("BG-123-BG");

        when(ticketDAO.saveTicket(any())).thenReturn(true);

       // Act - on appel la méthode processIncomingVehicle
       parkingService.processIncomingVehicle();

       // Assert
       verify(inputReaderUtil).readVehicleRegistrationNumber();
       verify(parkingSpotDAO).getNextAvailableSlot(ParkingType.CAR);
       verify(ticketDAO).saveTicket(any());
    }


    //Test 2
    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {

        // Arrange
        Ticket ticket = new Ticket();
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // Act
        boolean updateResult = ticketDAO.updateTicket(ticket);

        // Assert
        assertFalse(updateResult);
        verify(ticketDAO).updateTicket(ticket);

    }

    // Test 3
    @Test
    public void testGetNextParkingNumberIfAvailable(){

        //Arrange

        Ticket ticket = new Ticket();

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        //Act
        parkingService.getNextParkingNumberIfAvailable();

        // Assert
        verify(parkingSpotDAO).getNextAvailableSlot(ParkingType.CAR);

    }


    // Test 4 - retourn Error fetching parking number from DB. Parking slots might be full
    //    if(parkingNumber > 0){
    //        parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
    //    }else{
    //        throw new Exception("Error fetching parking number from DB. Parking slots might be full");
    //    }
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(){

        // Arrange
        Ticket ticket = new Ticket();

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        // Act
        parkingService.getNextParkingNumberIfAvailable();

        // Assert
        verify(parkingSpotDAO).getNextAvailableSlot(ParkingType.CAR);

    }


    // Test 5 avec un mauvais argument ( Type de véhicule = 3)
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {

        when(inputReaderUtil.readSelection()).thenReturn(3);

        ParkingSpot  parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        verify(inputReaderUtil, times(1)).readSelection();
        //assertNull(parkingSpot);
    }


    @Test
    public void testGetNextParkingNumberIfAvailable_ShouldReturnParkingSpot_WhenSlotAvailable() throws Exception {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(5);

        // Act
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        assertNotNull(spot);
        assertEquals(5, spot.getId());
        assertEquals(ParkingType.CAR, spot.getParkingType());
    }


}
