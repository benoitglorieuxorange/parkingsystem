package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount){

        double markdown = 1.0;

        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long intTimeMillis  = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        if (discount){
            markdown = 0.95;
        }

        long durationInMillis  = outTimeMillis - intTimeMillis;
        //Conversion to hour
        double duration = (double) durationInMillis / (60 * 60 * 1000);

        // Duration less than 30mn
        if (duration < 0.5) {
            ticket.setPrice(0);
            return;
        }


        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice((duration * Fare.CAR_RATE_PER_HOUR)*markdown);
                break;
            }
            case BIKE: {
                ticket.setPrice((duration * Fare.BIKE_RATE_PER_HOUR)*markdown);
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}