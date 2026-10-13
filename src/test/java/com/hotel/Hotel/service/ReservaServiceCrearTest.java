package com.hotel.Hotel.service;

import com.hotel.Hotel.domain.Cliente;
import com.hotel.Hotel.domain.Habitacion;
import com.hotel.Hotel.domain.HabitacionEstandar;
import com.hotel.Hotel.domain.Reserva;
import com.hotel.Hotel.dto.request.CrearReservaRequest;
import com.hotel.Hotel.dto.response.ReservaResponse;
import com.hotel.Hotel.exception.RecursoNoEncontradoException;
import com.hotel.Hotel.exception.ReglaNegocioException;
import com.hotel.Hotel.mapper.ReservaMapper;
import com.hotel.Hotel.repository.ClienteRepository;
import com.hotel.Hotel.repository.HabitacionRepository;
import com.hotel.Hotel.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceCrearTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private HabitacionRepository habitacionRepository;

    @Mock
    private ReservaMapper reservaMapper;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void creaReservaYCalculaElCostoTotal() {
        UUID clienteId = UUID.randomUUID();
        UUID habitacionId = UUID.randomUUID();
        Cliente cliente = new Cliente("Ana Perez", "ana@example.com");
        Habitacion habitacion = new HabitacionEstandar("101", 2, 150.0, 1);
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        LocalDateTime fin = inicio.plusDays(3);
        CrearReservaRequest request = new CrearReservaRequest(clienteId, habitacionId, inicio, fin);
        ReservaResponse response = new ReservaResponse(
                UUID.randomUUID(), "Ana Perez", "101", inicio, fin, 450.0, "PENDIENTE");

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(habitacionRepository.findById(habitacionId)).thenReturn(Optional.of(habitacion));
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaMapper.toResponse(any(Reserva.class))).thenReturn(response);

        ReservaResponse resultado = reservaService.crear(request);

        assertEquals(response, resultado);
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reservaCaptor.capture());
        assertEquals(450.0, reservaCaptor.getValue().getCostoTotal());
        assertEquals(cliente, reservaCaptor.getValue().getCliente());
        assertEquals(habitacion, reservaCaptor.getValue().getHabitacion());
        verify(reservaMapper).toResponse(reservaCaptor.getValue());
    }

    @Test
    void rechazaFechaDeSalidaAnteriorAFechaDeEntrada() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(3);
        CrearReservaRequest request = new CrearReservaRequest(
                UUID.randomUUID(), UUID.randomUUID(), inicio, inicio.minusDays(1));

        assertThrows(ReglaNegocioException.class, () -> reservaService.crear(request));

        verifyNoInteractions(clienteRepository, habitacionRepository, reservaRepository, reservaMapper);
    }

    @Test
    void fallaCuandoNoExisteElCliente() {
        UUID clienteId = UUID.randomUUID();
        CrearReservaRequest request = crearRequest(clienteId, UUID.randomUUID());
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crear(request));

        verify(clienteRepository).findById(clienteId);
        verifyNoInteractions(habitacionRepository, reservaRepository, reservaMapper);
    }

    @Test
    void fallaCuandoNoExisteLaHabitacion() {
        UUID clienteId = UUID.randomUUID();
        UUID habitacionId = UUID.randomUUID();
        CrearReservaRequest request = crearRequest(clienteId, habitacionId);
        when(clienteRepository.findById(clienteId))
                .thenReturn(Optional.of(new Cliente("Luis Diaz", "luis@example.com")));
        when(habitacionRepository.findById(habitacionId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crear(request));

        verify(clienteRepository).findById(clienteId);
        verify(habitacionRepository).findById(habitacionId);
        verifyNoInteractions(reservaRepository, reservaMapper);
    }

    @Test
    void rechazaCrearReservaParaClienteInactivo() {
        UUID clienteId = UUID.randomUUID();
        UUID habitacionId = UUID.randomUUID();
        Cliente cliente = new Cliente("Marta Ruiz", "marta@example.com");
        cliente.registrarPenalizacion();
        cliente.registrarPenalizacion();
        cliente.registrarPenalizacion();
        CrearReservaRequest request = crearRequest(clienteId, habitacionId);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(habitacionRepository.findById(habitacionId))
                .thenReturn(Optional.of(new HabitacionEstandar("202", 2, 120.0, 2)));

        assertThrows(ReglaNegocioException.class, () -> reservaService.crear(request));

        verifyNoInteractions(reservaRepository, reservaMapper);
    }

    private CrearReservaRequest crearRequest(UUID clienteId, UUID habitacionId) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        return new CrearReservaRequest(clienteId, habitacionId, inicio, inicio.plusDays(2));
    }
}
