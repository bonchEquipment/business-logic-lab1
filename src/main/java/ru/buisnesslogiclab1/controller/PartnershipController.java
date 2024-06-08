package ru.buisnesslogiclab1.controller;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.buisnesslogiclab1.config.HeaderConstant;
import ru.buisnesslogiclab1.dto.Response;
import ru.buisnesslogiclab1.dto.StatusCode;
import ru.buisnesslogiclab1.service.PartnershipService;
import ru.buisnesslogiclab1.service.UserService;
import ru.buisnesslogiclab1.util.ResponseHelper;
import ru.buisnesslogiclab1.validation.IdValidator;
import ru.buisnesslogiclab1.validation.user.ValidUserId;

@Validated
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/partnership")
public class PartnershipController {


    private final PartnershipService service;
    private final ResponseHelper responseHelper;
    private final IdValidator idValidator;
    private final UserService userService;


    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    @PostMapping("/withdrawMoney")
    public ResponseEntity<Response<StatusCode>> withdrawMoney(
            @RequestHeader(value = HeaderConstant.AMOUNT, required = true)
            Integer amount) {
        var user = userService.findUserEntityForCurrentSession();
        if (user == null)
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_USER);

        try {
           service.withdrawSubscriptionMoney(BigDecimal.valueOf(amount), user.getId());
            return responseHelper.asResponseEntity(StatusCode.OK);
        }  catch (Exception e){
            log.info(e.getMessage(), e);
            return responseHelper.asResponseEntity(StatusCode.createRequestFailedCode(e.getMessage()));
        }
    }

}
