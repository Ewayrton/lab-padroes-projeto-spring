package one.digitalinnovation.gof.service.impl;

import one.digitalinnovation.gof.dto.ClienteDTO;
import one.digitalinnovation.gof.model.Cliente;
import one.digitalinnovation.gof.model.Endereco;
import one.digitalinnovation.gof.repository.ClienteRepository;
import one.digitalinnovation.gof.repository.EnderecoRepository;
import one.digitalinnovation.gof.service.ViaCepService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private EnderecoRepository enderecoRepository;

    @Mock
    private ViaCepService viaCepService;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    // --- Teste de Busca ---

    @Test
    void deveBuscarTodosOsClientes() {
        // Arrange (Preparação)
        clienteService.buscarTodos();

        // Assert (Verificação)
        // Apenas verifica se o método do repositório foi chamado
        verify(clienteRepository, times(1)).findAll();
    }

    @Test
    void deveBuscarClientePorId() {
        // Arrange
        Long id = 1L;
        Cliente clienteMock = new Cliente();
        clienteMock.setId(id);
        when(clienteRepository.findById(id)).thenReturn(Optional.of(clienteMock));

        // Act (Ação)
        Cliente resultado = clienteService.buscarPorId(id);

        // Assert
        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(id, resultado.getId());
        verify(clienteRepository, times(1)).findById(id);
    }

    // --- Teste de Inserção (A mágica do CEP) ---

    @Test
    void deveInserirClienteComCepNovo() {
        // Cenário: O cliente passa um CEP que AINDA NÃO existe no nosso banco.
        // O sistema deve chamar o ViaCEP.

        // Arrange
        String cep = "12345678";
        ClienteDTO dto = new ClienteDTO("João", cep);

        Endereco enderecoViaCep = new Endereco();
        enderecoViaCep.setCep(cep);
        enderecoViaCep.setLogradouro("Rua Nova");

        // Mock 1: Endereço não encontrado no banco local
        when(enderecoRepository.findById(cep)).thenReturn(Optional.empty());
        // Mock 2: ViaCEP retorna o endereço completo
        when(viaCepService.consultarCep(cep)).thenReturn(enderecoViaCep);
        // Mock 3: Save do repository apenas retorna o objeto que recebeu (pra não dar erro)
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.inserir(dto);

        // Assert
        Assertions.assertEquals("João", resultado.getNome());
        Assertions.assertEquals("Rua Nova", resultado.getEndereco().getLogradouro());

        // Verificações Cruciais:
        verify(viaCepService, times(1)).consultarCep(cep); // Chamou o ViaCEP?
        verify(enderecoRepository, times(1)).save(enderecoViaCep); // Salvou o novo endereço?
        verify(clienteRepository, times(1)).save(any(Cliente.class)); // Salvou o cliente?
    }

    @Test
    void deveInserirClienteComCepExistente() {
        // Cenário: O CEP já existe no banco. O sistema NÃO deve chamar o ViaCEP.

        // Arrange
        String cep = "87654321";
        ClienteDTO dto = new ClienteDTO("Maria", cep);

        Endereco enderecoExistente = new Endereco();
        enderecoExistente.setCep(cep);
        enderecoExistente.setLogradouro("Rua Antiga");

        // Mock: Endereço JÁ encontrado no banco local
        when(enderecoRepository.findById(cep)).thenReturn(Optional.of(enderecoExistente));

        // Mock do save
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.inserir(dto);

        // Assert
        Assertions.assertEquals("Rua Antiga", resultado.getEndereco().getLogradouro());

        // Verificação Crucial: NÃO deve chamar o ViaCEP
        verify(viaCepService, never()).consultarCep(anyString());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }
}