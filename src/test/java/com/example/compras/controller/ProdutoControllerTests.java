package com.example.compras.controller;

import com.example.compras.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:compras-testes")
@AutoConfigureMockMvc
class ProdutoControllerTests {

    private static final String PRODUTO = """
            {
              "id": 999,
              "nome": "Notebook",
              "descricao": "Notebook para estudos",
              "preco": 3500.00,
              "quantidade": 2
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void limparBanco() {
        produtoRepository.deleteAll();
    }

    @Test
    void deveCadastrarListarBuscarAtualizarEExcluirProduto() throws Exception {
        String localizacao = mockMvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Notebook"))
                .andExpect(jsonPath("$.descricao").value("Notebook para estudos"))
                .andExpect(jsonPath("$.preco").value(3500.0))
                .andExpect(jsonPath("$.quantidade").value(2))
                .andReturn().getResponse().getHeader("Location");

        Long id = produtoRepository.findAll().getFirst().getId();
        assertThat(id).isNotEqualTo(999L);
        assertThat(localizacao).isEqualTo("/produtos/" + id);

        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get(localizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        String atualizacao = """
                {
                  "id": 888,
                  "nome": "Notebook atualizado",
                  "descricao": "Notebook para estudos e trabalho",
                  "preco": 3200.00,
                  "quantidade": 5
                }
                """;

        mockMvc.perform(put(localizacao)
                        .contentType(MediaType.APPLICATION_JSON).content(atualizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Notebook atualizado"))
                .andExpect(jsonPath("$.descricao").value("Notebook para estudos e trabalho"))
                .andExpect(jsonPath("$.preco").value(3200.0))
                .andExpect(jsonPath("$.quantidade").value(5));

        mockMvc.perform(get(localizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(3200.0));
        assertThat(produtoRepository.count()).isEqualTo(1);

        mockMvc.perform(delete(localizacao))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get(localizacao)).andExpect(status().isNotFound());
        assertThat(produtoRepository.count()).isZero();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaProdutos() throws Exception {
        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void deveRetornar404AoBuscarProdutoInexistente() throws Exception {
        mockMvc.perform(get("/produtos/999")).andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar404AoAtualizarProdutoInexistente() throws Exception {
        mockMvc.perform(put("/produtos/999")
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUTO))
                .andExpect(status().isNotFound());
        assertThat(produtoRepository.count()).isZero();
    }

    @Test
    void deveRetornar404AoExcluirProdutoInexistente() throws Exception {
        mockMvc.perform(delete("/produtos/999")).andExpect(status().isNotFound());
    }
}
