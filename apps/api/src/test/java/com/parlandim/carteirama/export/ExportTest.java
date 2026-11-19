package com.parlandim.carteirama.export;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTest extends IntegrationTest {

    @Test
    void exportaCsvEXlsxMesmoComContaSuspensa() {
        Api api = registered();
        String g = api.post("/api/v1/groups", Map.of("name", "Projeto; X")).json().path("id").asString();
        api.post("/api/v1/clients", Api.body("name", "=Ana", "group_id", g, "amount_cents", 5900, "due_day", 10));
        api.post("/api/v1/charges/generate", Map.of("month", "2026-12"));
        setAccountStatus(accountIdOf(api), "suspended");

        var csv = api.get("/api/v1/export?type=charges&format=csv&month=2026-12");
        assertThat(csv.status()).isEqualTo(200);
        assertThat(csv.headers().firstValue("content-type").orElseThrow()).startsWith("text/csv");
        assertThat(csv.headers().firstValue("content-disposition").orElseThrow())
                .contains("carteirama-cobrancas-2026-12.csv");
        assertThat(csv.raw()).contains("Cliente;Grupo;Mês;Cobrança;Vencimento;Serviço (R$);Bônus (R$);Valor cobrado (R$);Custos (R$);Líquido (R$);Status;Pago em;Nota");
        assertThat(csv.raw()).contains("'=Ana;\"Projeto; X\";2026-12;Mensalidade;10/12/2026;59,00;0,00;59,00;0,00;59,00;Pendente;;");

        var xlsx = api.get("/api/v1/export?type=clients&format=xlsx");
        assertThat(xlsx.status()).isEqualTo(200);
        assertThat(xlsx.headers().firstValue("content-disposition").orElseThrow()).contains("carteirama-clientes.xlsx");
        assertThat(xlsx.raw()).startsWith("PK");

        assertThat(api.get("/api/v1/export?type=groups&format=csv").status()).isEqualTo(200);
    }

    @Test
    void variosTiposViramUmaTabelaSo() {
        Api api = registered();
        String g = api.post("/api/v1/groups", Map.of("name", "Saas", "notes", "Plano mensal")).json().path("id").asString();
        api.post("/api/v1/groups", Map.of("name", "Vazio"));
        api.post("/api/v1/clients", Api.body("name", "Ana", "group_id", g, "amount_cents", 5900, "due_day", 10));
        api.post("/api/v1/clients", Api.body("name", "Bia", "group_id", g, "amount_cents", 1000, "due_day", 5));
        api.post("/api/v1/charges/generate", Map.of("month", "2026-12"));
        String ana = api.get("/api/v1/clients?q=Ana").json().path("data").get(0).path("id").asString();
        api.post("/api/v1/charges", Api.body("client_id", ana, "kind", "one_off", "description", "Instalação",
                "amount_cents", 15000, "due_date", "2026-12-20"));

        var all = api.get("/api/v1/export?type=clients&type=groups&type=charges&format=csv&month=2026-12");
        assertThat(all.status()).isEqualTo(200);
        assertThat(all.headers().firstValue("content-disposition").orElseThrow())
                .contains("carteirama-clientes-grupos-cobrancas-2026-12.csv");
        String[] lines = all.raw().replace("﻿", "").split("\r\n");
        assertThat(lines[0]).isEqualTo("Cliente;Documento;E-mail;Telefone;Assinatura (R$);Dia de vencimento;"
                + "Status do cliente;Observações do cliente;Grupo;Cor do grupo;Clientes no grupo;"
                + "Total do grupo no mês (R$);Descrição do grupo;Mês;Cobrança;Vencimento;Serviço (R$);Bônus (R$);"
                + "Valor cobrado (R$);Custos (R$);Líquido (R$);Situação da cobrança;Pago em;Nota da cobrança");
        assertThat(lines).hasSize(5);
        assertThat(lines[1]).isEqualTo("Ana;;;;59,00;10;Ativo;;Saas;;2;219,00;Plano mensal;2026-12;Mensalidade;"
                + "10/12/2026;59,00;0,00;59,00;0,00;59,00;Pendente;;");
        assertThat(lines[2]).isEqualTo("Ana;;;;59,00;10;Ativo;;Saas;;2;219,00;Plano mensal;2026-12;Instalação;"
                + "20/12/2026;150,00;0,00;150,00;0,00;150,00;Pendente;;");
        assertThat(lines[3]).startsWith("Bia;");
        assertThat(lines[4]).isEqualTo(";;;;;;;;Vazio;;0;0,00;;;;;;;;;;;;");

        var chargesAndGroups = api.get("/api/v1/export?type=charges,groups&format=csv&month=2026-12");
        assertThat(chargesAndGroups.raw()).contains("Cliente;Grupo;Cor do grupo;Clientes no grupo;");
        assertThat(chargesAndGroups.raw())
                .contains("Ana;Saas;;2;219,00;Plano mensal;2026-12;Mensalidade;10/12/2026;59,00;0,00;59,00;0,00;59,00;Pendente;;")
                .contains("Ana;Saas;;2;219,00;Plano mensal;2026-12;Instalação;20/12/2026;150,00;0,00;150,00;0,00;150,00;Pendente;;");

        assertThat(api.get("/api/v1/export?type=clients&type=groups&format=xlsx").raw()).startsWith("PK");
    }

    @Test
    void validaParametros() {
        Api api = registered();
        assertThat(api.get("/api/v1/export?type=clients&format=pdf").details()).containsExactly("format:invalid_value");
        assertThat(api.get("/api/v1/export?type=x&format=csv").details()).containsExactly("type:invalid_value");
        assertThat(api.get("/api/v1/export?type=clients").details()).containsExactly("format:required");
    }

    @Test
    void exportaSoDadosDaPropriaConta() {
        Api a = registered();
        Api b = registered();
        String g = a.post("/api/v1/groups", Map.of("name", "G")).json().path("id").asString();
        a.post("/api/v1/clients", Api.body("name", "Cliente da A", "group_id", g, "amount_cents", 100, "due_day", 1));

        assertThat(b.get("/api/v1/export?type=clients&format=csv").raw()).doesNotContain("Cliente da A");
    }
}
