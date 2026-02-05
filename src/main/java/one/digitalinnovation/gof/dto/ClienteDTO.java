package one.digitalinnovation.gof.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClienteDTO(
        @NotBlank(message = "O nome do cliente é obrigatório")
        String nome,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter apenas 8 números")
        String cep
) { }