package com.akolodziejski.divstock.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity(name="stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticker;

    @OneToMany(cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "stock")
    private List<Rank> ranks;
}
