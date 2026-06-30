package co.cambodia.farmer.mapper;




import co.cambodia.farmer.domain.Payment;
import co.cambodia.farmer.dto.peyment.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionHash(),
                payment.getMd5Hash(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getOrder() != null ? payment.getOrder().getId() : null
        );
    }
}