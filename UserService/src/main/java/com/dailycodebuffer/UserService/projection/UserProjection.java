package com.dailycodebuffer.UserService.projection;

import com.dailycodebuffer.CommonService.model.CardDetails;
import com.dailycodebuffer.CommonService.model.User;
import com.dailycodebuffer.CommonService.queries.GetUserPaymentDetailsQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class UserProjection {

    @QueryHandler
    public User getUserPaymentDetails(GetUserPaymentDetailsQuery query) {
        //Ideally Get the details from the DB
        // Here we are hardcoding the details for the sake of simplicity
        CardDetails cardDetails
                = CardDetails.builder()
                .name("Qiang Li")
                .validUntilYear(2028)
                .validUntilMonth(05)
                .cardNumber("123456789")
                .cvv(111)
                .build();

        return User.builder()
                .userId(query.getUserId())
                .firstName("Qiang")
                .lastName("Li")
                .cardDetails(cardDetails)
                .build();
    }
}
