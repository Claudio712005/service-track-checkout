CREATE SCHEMA IF NOT EXISTS CHECKOUT;

SET SEARCH_PATH TO CHECKOUT;

CREATE TABLE IF NOT EXISTS COBRANCAS (
    ID UUID NOT NULL,
    ORDEM_SERVICO_ID UUID NOT NULL,
    MEIO VARCHAR(20) NOT NULL,
    VALOR NUMERIC(12,2) NOT NULL,
    SITUACAO VARCHAR(20) NOT NULL,
    PAGAMENTO_EXTERNO_ID BIGINT,
    MOTIVO VARCHAR(500),
    VERSAO INTEGER NOT NULL DEFAULT 0,
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT UQ_COBRANCAS_ORDEM_ABERTA UNIQUE (ORDEM_SERVICO_ID, MEIO),
    CONSTRAINT CK_COBRANCAS_VALOR CHECK (VALOR > 0)
);

COMMENT ON TABLE COBRANCAS IS 'Cobranca de uma ordem de servico. O estado aqui e nosso, nao do provedor: o provedor e consultado e notifica, mas quem decide o que a plataforma entende por aprovado e este servico.';
COMMENT ON COLUMN COBRANCAS.ORDEM_SERVICO_ID IS 'Referencia opaca ao service-track-ordens. Sem chave estrangeira: banco de outro servico.';
COMMENT ON COLUMN COBRANCAS.MEIO IS 'PIX, BOLETO ou CARTAO. Pix e boleto sao assincronos e so confirmam por notificacao; cartao responde na propria chamada.';
COMMENT ON COLUMN COBRANCAS.SITUACAO IS 'PENDENTE, EM_ANALISE, APROVADA, RECUSADA, CANCELADA ou ESTORNADA. Derivada do status do provedor, nao copiada dele.';
COMMENT ON COLUMN COBRANCAS.PAGAMENTO_EXTERNO_ID IS 'Identificador do pagamento no provedor. Nulo entre a criacao da cobranca e a resposta da chamada: se o processo morrer no meio, a cobranca fica pendente sem id e e reconciliada por consulta.';
COMMENT ON COLUMN COBRANCAS.MOTIVO IS 'status_detail do provedor na recusa. E o que o atendente le para explicar ao cliente.';
COMMENT ON COLUMN COBRANCAS.VERSAO IS 'Trava otimista. Notificacao do provedor e consulta de reconciliacao podem chegar juntas.';
COMMENT ON CONSTRAINT UQ_COBRANCAS_ORDEM_ABERTA ON COBRANCAS IS 'Uma cobranca por meio por ordem. Clicar duas vezes em pagar nao abre duas cobrancas; trocar de meio abre outra, e a anterior e cancelada.';

CREATE INDEX IF NOT EXISTS IX_COBRANCAS_ORDEM ON COBRANCAS (ORDEM_SERVICO_ID, DATA_CRIACAO DESC);
CREATE UNIQUE INDEX IF NOT EXISTS UQ_COBRANCAS_PAGAMENTO_EXTERNO ON COBRANCAS (PAGAMENTO_EXTERNO_ID) WHERE PAGAMENTO_EXTERNO_ID IS NOT NULL;

COMMENT ON INDEX UQ_COBRANCAS_PAGAMENTO_EXTERNO IS 'A notificacao do provedor traz o id dele, nao o nosso. Sem este indice a busca pela notificacao varreria a tabela, e dois registros com o mesmo id externo passariam.';

CREATE TABLE IF NOT EXISTS NOTIFICACOES (
    ID VARCHAR(120) NOT NULL,
    TIPO VARCHAR(40) NOT NULL,
    ACAO VARCHAR(60),
    RECURSO_ID VARCHAR(60) NOT NULL,
    DATA_PROCESSAMENTO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID)
);

COMMENT ON TABLE NOTIFICACOES IS 'INBOX das notificacoes do provedor. O Mercado Pago reenvia a mesma notificacao ate receber 2xx, entao sem isto um reenvio reaplicaria o efeito.';
COMMENT ON COLUMN NOTIFICACOES.ID IS 'Chave de idempotencia: <tipo>:<acao>:<recursoId>. A mesma transicao reenviada colide na chave primaria.';
