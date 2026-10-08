package com.teo1.clinicadental.model;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiaSemanaTests {

    @Test
    void convierteCadaDiaDeLaSemana() {
        assertEquals(DiaSemana.LUNES, DiaSemana.desde(DayOfWeek.MONDAY));
        assertEquals(DiaSemana.MARTES, DiaSemana.desde(DayOfWeek.TUESDAY));
        assertEquals(DiaSemana.MIERCOLES, DiaSemana.desde(DayOfWeek.WEDNESDAY));
        assertEquals(DiaSemana.JUEVES, DiaSemana.desde(DayOfWeek.THURSDAY));
        assertEquals(DiaSemana.VIERNES, DiaSemana.desde(DayOfWeek.FRIDAY));
        assertEquals(DiaSemana.SABADO, DiaSemana.desde(DayOfWeek.SATURDAY));
        assertEquals(DiaSemana.DOMINGO, DiaSemana.desde(DayOfWeek.SUNDAY));
    }
}
