package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability

        // Act
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        // Assert
        assertNotNull(ticket);

        // Act - récupere le prochain SPOT disponible
        int nextSpotAvailable = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        // Assert - vérifie que le prochain SPOT disponible est un entier > à 0
        assertTrue(nextSpotAvailable > 0);

        parkingService.processExitingVehicle();

    }

    @Test
    public void testParkingLotExit() throws InterruptedException {
        testParkingACar();

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        //TODO: check that the fare generated and out time are populated correctly in the database

        parkingService.processIncomingVehicle();

        Thread.sleep(1000);

       parkingService.processExitingVehicle();
       Ticket ticket =  ticketDAO.getTicket("ABCDEF");

       assertNotNull(ticket, "Le ticket existe dans la base de donnée");
       assertNotNull(ticket.getInTime(), "la date et l'heure d'entrée ne sont pas null");
       assertNotNull(ticket.getOutTime(),"La date et l'heure de sortie ne sont pas null");

        int duration = 0;
        if (duration <= 30) {
            assertEquals( 0, ticket.getPrice(), 0.01, "Le tarif devrait être gratuit pour les 30 premières minutes");
        } else {
            assertTrue(ticket.getPrice() > 0, "Au dela de 30 minute le tarif doit être supérieur a 0");
        }
    }


    @Test
    public void testParkingLotExitRecurringUser() throws InterruptedException {

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // creation d'une nouvelle entrée
        parkingService.processIncomingVehicle();
        Thread.sleep(1000);
        parkingService.processExitingVehicle();

        Ticket ticket =  ticketDAO.getTicket("ABCDEF");

        //on verifie que le ticket du vehicule ABCDEF existe deja
        assertEquals(1, ticket.getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());

        System.out.println("Le tarif du premier ticket est : " + ticket.getPrice());

        parkingService.processIncomingVehicle();

        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = dataBaseTestConfig.getConnection();

            // Préparer la requête
            String sql = "UPDATE ticket SET IN_TIME = DATE_SUB(IN_TIME, INTERVAL 45 MINUTE) WHERE id = 2;";
            pstmt = connection.prepareStatement(sql);

            // Exécuter la requête
            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Fermer le PreparedStatement
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Fermer la connexion
            dataBaseTestConfig.closeConnection(connection);
        }

        parkingService.processExitingVehicle();

        Ticket ticket2 = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket2.getOutTime());

        System.out.println(ticket2.getInTime() + " " +  ticket2.getOutTime());

        long intTimeMillis  = (ticket2.getInTime().getTime());
        long outTimeMillis = (ticket2.getOutTime().getTime());
        long durationInMillis = outTimeMillis - intTimeMillis;

        double duration = (double) durationInMillis / (1000 * 60); //durée en minutes
        System.out.println("duration: " + duration);
        System.out.println("Prix du ticket : " + ticket2.getPrice());

        if (duration < 30){
            assertEquals(0, ticket2.getPrice(), 0.01);
        }else{
            assertTrue(ticket2.getPrice() > 0);
        }




    }

}
