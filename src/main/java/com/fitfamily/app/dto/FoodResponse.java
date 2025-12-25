package com.fitfamily.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {

	private UUID id;
	private String name;
	private List<FoodPortionResponse> portions;

}

