package com.easylink.easylink.entities;

import com.easylink.easylink.enums.TypePhone;
import jakarta.persistence.*;

@Entity
public class PhoneNumber extends AbstractContactDetail{

    private String number;

    @Enumerated(EnumType.STRING)
    private TypePhone type;

}
