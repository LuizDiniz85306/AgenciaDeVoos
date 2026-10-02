"""Gera a documentação acadêmica de entrega da Fase II em oito páginas."""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    Flowable, Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
)


ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "output" / "pdf" / "Fase II - Decisoes de Projeto.pdf"
BRASAO_PUC = ROOT / "docs" / "assets" / "brasao-puc-minas.png"

AZUL = colors.HexColor("#183F6A")
AZUL_MEDIO = colors.HexColor("#006E8A")
AZUL_CLARO = colors.HexColor("#DCEEF7")
VERDE_CLARO = colors.HexColor("#E7F6EF")
AMARELO_CLARO = colors.HexColor("#FFF3C4")
LILAS_CLARO = colors.HexColor("#EEE8FF")
TEXTO = colors.HexColor("#24364B")
LINHA = colors.HexColor("#BED0DF")
FUNDO = colors.HexColor("#F6F9FB")


class Linha(Flowable):
    def __init__(self, cor=AZUL_MEDIO):
        super().__init__()
        self.width, self.height, self.cor = 16.2 * cm, 0.12 * cm, cor

    def draw(self):
        self.canv.setStrokeColor(self.cor)
        self.canv.setLineWidth(0.7)
        self.canv.line(0, self.height / 2, self.width, self.height / 2)


class ModeloDados(Flowable):
    def __init__(self):
        super().__init__()
        self.width, self.height = 16.2 * cm, 5.9 * cm

    def caixa(self, c, x, titulo, linhas):
        y, w, h = 1.55 * cm, 5.65 * cm, 3.85 * cm
        c.setFillColor(AZUL_CLARO)
        c.setStrokeColor(AZUL)
        c.roundRect(x, y, w, h, 9, fill=1, stroke=1)
        c.setFillColor(AZUL)
        c.roundRect(x, y + h - 0.78 * cm, w, 0.78 * cm, 9, fill=1, stroke=0)
        c.setFillColor(colors.white)
        c.setFont("Helvetica-Bold", 11)
        c.drawCentredString(x + w / 2, y + h - 0.52 * cm, titulo)
        c.setFillColor(TEXTO)
        c.setFont("Helvetica", 9.2)
        for i, texto in enumerate(linhas):
            c.drawString(x + 0.35 * cm, y + h - 1.32 * cm - i * 0.57 * cm, texto)

    def draw(self):
        c = self.canv
        self.caixa(c, 0, "CLIENTE", ["id (PK)", "nome", "cpf", "telefone", "e-mail"])
        self.caixa(c, 10.55 * cm, "VOO", ["id (PK)", "idCliente (FK -> Cliente.id)", "código", "origem e destino", "data, horário, valor e status"])
        y = 0.92 * cm
        c.setStrokeColor(AZUL_MEDIO)
        c.setLineWidth(1.5)
        c.line(5.65 * cm, y, 10.55 * cm, y)
        c.setFillColor(AZUL_MEDIO)
        c.setFont("Helvetica-Bold", 11)
        c.drawString(5.88 * cm, y + 0.14 * cm, "1")
        c.drawRightString(10.32 * cm, y + 0.14 * cm, "N")
        c.setFillColor(TEXTO)
        c.setFont("Helvetica", 8.7)
        c.drawCentredString(self.width / 2, 0.30 * cm, "Um cliente pode ter vários voos; cada voo referencia no máximo um cliente.")


class Arquitetura(Flowable):
    def __init__(self):
        super().__init__()
        self.width, self.height = 16.2 * cm, 8.0 * cm

    def camada(self, c, y, cor, nome, componente, detalhe):
        c.setFillColor(cor)
        c.setStrokeColor(AZUL)
        c.roundRect(0.15 * cm, y, 15.9 * cm, 1.3 * cm, 8, fill=1, stroke=1)
        c.setFillColor(AZUL)
        c.setFont("Helvetica-Bold", 9.5)
        c.drawString(0.52 * cm, y + 0.84 * cm, nome)
        c.setFillColor(colors.black)
        c.setFont("Helvetica-Bold", 9.0)
        c.drawString(3.5 * cm, y + 0.84 * cm, componente)
        c.setFillColor(TEXTO)
        c.setFont("Helvetica", 8.5)
        c.drawString(3.5 * cm, y + 0.40 * cm, detalhe)

    def draw(self):
        c = self.canv
        self.camada(c, 6.35 * cm, AZUL_CLARO, "Apresentação", "Principal, TelaClientes e TelaVoos", "Formulários Swing, CRUD e demonstração visual")
        self.camada(c, 4.55 * cm, VERDE_CLARO, "Aplicação", "AgenciaService e ClienteDAO", "Integridade referencial e coordenação das operações")
        self.camada(c, 2.75 * cm, AMARELO_CLARO, "Domínio", "Cliente, Voo e Registro", "PK, FK e serialização dos registros")
        self.camada(c, 0.95 * cm, LILAS_CLARO, "Infraestrutura", "Arquivo, HashExtensivel e ArvoreBMais", "Arquivos binários, índices e ordenação externa")
        c.setStrokeColor(AZUL_MEDIO)
        c.setLineWidth(1.2)
        for y in [6.02 * cm, 4.22 * cm, 2.42 * cm]:
            c.line(8.1 * cm, y, 8.1 * cm, y - 0.34 * cm)
            c.line(8.1 * cm, y - 0.34 * cm, 7.94 * cm, y - 0.16 * cm)
            c.line(8.1 * cm, y - 0.34 * cm, 8.26 * cm, y - 0.16 * cm)


class TresBlocos(Flowable):
    """Diagrama para Hash, B+ ou ordenação, com três estágios bem identificados."""
    def __init__(self, titulos, descricoes, legenda):
        super().__init__()
        self.width, self.height = 16.2 * cm, 4.7 * cm
        self.titulos, self.descricoes, self.legenda = titulos, descricoes, legenda

    def draw(self):
        c = self.canv
        cores = [AZUL_CLARO, VERDE_CLARO, AMARELO_CLARO]
        xs = [0, 5.55 * cm, 11.10 * cm]
        for x, titulo, linhas, cor in zip(xs, self.titulos, self.descricoes, cores):
            c.setFillColor(cor)
            c.setStrokeColor(AZUL)
            c.roundRect(x, 1.08 * cm, 5.1 * cm, 2.62 * cm, 8, fill=1, stroke=1)
            c.setFillColor(AZUL)
            c.setFont("Helvetica-Bold", 9.4)
            c.drawCentredString(x + 2.55 * cm, 3.14 * cm, titulo)
            c.setFillColor(TEXTO)
            c.setFont("Helvetica", 8.5)
            for i, linha in enumerate(linhas):
                c.drawCentredString(x + 2.55 * cm, 2.40 * cm - i * 0.48 * cm, linha)
        c.setStrokeColor(AZUL_MEDIO)
        c.setLineWidth(1.4)
        for x in [5.1 * cm, 10.65 * cm]:
            c.line(x + 0.18 * cm, 2.40 * cm, x + 0.54 * cm, 2.40 * cm)
            c.line(x + 0.54 * cm, 2.40 * cm, x + 0.36 * cm, 2.58 * cm)
            c.line(x + 0.54 * cm, 2.40 * cm, x + 0.36 * cm, 2.22 * cm)
        c.setFillColor(TEXTO)
        c.setFont("Helvetica", 8.6)
        c.drawCentredString(self.width / 2, 0.35 * cm, self.legenda)


def estilos():
    base = getSampleStyleSheet()
    return {
        "capa_titulo": ParagraphStyle("capa_titulo", parent=base["Title"], fontName="Times-Bold", fontSize=30, leading=36, alignment=TA_CENTER, textColor=AZUL),
        "capa_label": ParagraphStyle("capa_label", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=8.2, leading=10, alignment=TA_CENTER, textColor=AZUL_MEDIO, spaceBefore=8),
        "capa_valor": ParagraphStyle("capa_valor", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=10.4, leading=14, alignment=TA_CENTER, textColor=AZUL),
        "titulo": ParagraphStyle("titulo", parent=base["Heading1"], fontName="Helvetica-Bold", fontSize=20, leading=24, textColor=AZUL, spaceAfter=8),
        "subtitulo": ParagraphStyle("subtitulo", parent=base["Heading2"], fontName="Helvetica-Bold", fontSize=13.2, leading=17, textColor=AZUL, spaceBefore=7, spaceAfter=5),
        "corpo": ParagraphStyle("corpo", parent=base["BodyText"], fontName="Helvetica", fontSize=10.1, leading=14.0, textColor=TEXTO, spaceAfter=8),
        "nota": ParagraphStyle("nota", parent=base["BodyText"], fontName="Helvetica", fontSize=9.4, leading=12.8, textColor=TEXTO, backColor=FUNDO, borderColor=LINHA, borderWidth=0.7, borderPadding=9, spaceBefore=4, spaceAfter=9),
        "celula": ParagraphStyle("celula", parent=base["BodyText"], fontName="Helvetica", fontSize=8.7, leading=11.1, textColor=TEXTO),
        "celula_h": ParagraphStyle("celula_h", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=8.6, leading=10.5, textColor=colors.white),
    }


def p(texto, estilo):
    return Paragraph(texto, estilo)


def tabela(linhas, larguras, e):
    dados = []
    for i, linha in enumerate(linhas):
        estilo = e["celula_h"] if i == 0 else e["celula"]
        dados.append([p(celula, estilo) for celula in linha])
    resultado = Table(dados, colWidths=larguras, repeatRows=1, hAlign="LEFT")
    resultado.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), AZUL),
        ("BACKGROUND", (0, 1), (-1, -1), FUNDO),
        ("GRID", (0, 0), (-1, -1), 0.45, LINHA),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    return resultado


def cabecalho(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(AZUL_MEDIO)
    canvas.setLineWidth(0.7)
    canvas.line(2.0 * cm, A4[1] - 1.35 * cm, A4[0] - 2.0 * cm, A4[1] - 1.35 * cm)
    canvas.setFillColor(AZUL)
    canvas.setFont("Helvetica-Bold", 8.2)
    canvas.drawString(2.0 * cm, A4[1] - 1.02 * cm, "AGÊNCIA DE VOOS")
    canvas.setFillColor(TEXTO)
    canvas.setFont("Helvetica", 8.2)
    canvas.drawRightString(A4[0] - 2.0 * cm, A4[1] - 1.02 * cm, "AED III - Trabalho Prático - Fase II")
    canvas.setStrokeColor(LINHA)
    canvas.setLineWidth(0.45)
    canvas.line(2.0 * cm, 1.22 * cm, A4[0] - 2.0 * cm, 1.22 * cm)
    canvas.setFillColor(TEXTO)
    canvas.setFont("Helvetica", 8.2)
    canvas.drawString(2.0 * cm, 0.82 * cm, "AED III - Trabalho Prático - Fase II | Agência de Voos")
    canvas.drawRightString(A4[0] - 2.0 * cm, 0.82 * cm, f"Página {doc.page}")
    canvas.restoreState()


def rodape_capa(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(TEXTO)
    canvas.setFont("Helvetica", 8.5)
    canvas.drawCentredString(A4[0] / 2, 0.85 * cm, "AED III - Trabalho Prático")
    canvas.restoreState()


def gerar():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    e = estilos()
    s = []

    # Capa
    s += [
        Spacer(1, 3.0 * cm), p("AGÊNCIA DE VOOS", e["capa_titulo"]),
        Spacer(1, 1.0 * cm),
        Image(str(BRASAO_PUC), width=4.4 * cm, height=4.4 * cm * 618 / 712,
              hAlign="CENTER", mask="auto"),
        Spacer(1, 0.30 * cm),
        p("DISCIPLINA", e["capa_label"]), p("AED III", e["capa_valor"]),
        p("ETAPA", e["capa_label"]), p("Fase II - Estruturas de indexação e ordenação externa", e["capa_valor"]),
        p("INTEGRANTES", e["capa_label"]), p("Luiz Henrique de Paula Diniz", e["capa_valor"]), p("Eder Arthur", e["capa_valor"]),
        p("REPOSITÓRIO DO PROJETO", e["capa_label"]), p("github.com/LuizDiniz85306/AgenciaDeVoos", e["capa_valor"]),
        PageBreak(),
    ]

    # Apresentação
    s += [
        p("Apresentação", e["titulo"]), Linha(), Spacer(1, 0.22 * cm),
        p("Este documento registra as decisões de projeto e as evidências de implementação da Fase II da aplicação Agência de Voos. O foco desta etapa é manter os dados íntegros, localizáveis e ordenáveis em memória secundária, inclusive após a reinicialização da aplicação.", e["corpo"]),
        p("Objetivos da Fase II", e["subtitulo"]),
        tabela([
            ["Entrega", "Aplicação no projeto"],
            ["CRUD e relacionamento 1:N", "Cliente é o lado 1 e Voo é o lado N; a FK idCliente é armazenada no próprio voo."],
            ["Hash Extensível", "Busca direta de Cliente e Voo pela PK, com diretório e buckets gravados em arquivos binários."],
            ["Árvore B+", "Consulta por idCliente e recuperação de todos os IDs de voos associados à FK."],
            ["Ordenação externa", "Geração de runs e intercalação em disco por data/hora, preservando o arquivo principal."],
        ], [5.1 * cm, 11.1 * cm], e),
        p("Organização do documento", e["subtitulo"]),
        tabela([
            ["Seção", "Conteúdo"],
            ["1. Modelo de dados", "Entidades, chave estrangeira e regras de integridade referencial."],
            ["2. Arquitetura", "Responsabilidades das telas, serviços, entidades e arquivos binários."],
            ["3. Hash Extensível", "Persistência do índice de chave primária e manutenção no CRUD."],
            ["4. Árvore B+", "Estrutura de consulta por FK e resposta para ocorrências repetidas."],
            ["5. Ordenação externa", "Runs, intercalação balanceada e arquivo final ordenado."],
            ["6. Integridade", "Sincronização entre dados e índices e justificativas das decisões."],
        ], [5.1 * cm, 11.1 * cm], e),
        Spacer(1, 0.14 * cm),
        p("<b>Observação de escopo.</b> A associação legada VooCliente era N:N. Nesta fase, ela foi consolidada em Voo.idCliente para atender ao relacionamento 1:N solicitado. Uma migração única preserva a primeira associação ativa de cada voo sem apagar o arquivo antigo.", e["nota"]),
        PageBreak(),
    ]

    # Modelo e arquitetura
    s += [
        p("1. Modelo de dados e relacionamento", e["titulo"]), Linha(), Spacer(1, 0.28 * cm),
        p("As entidades persistidas nesta fase são Cliente e Voo. A chave estrangeira foi posicionada no lado N: cada voo armazena idCliente. Assim, um cliente pode aparecer em diversos voos sem a necessidade de tabela intermediária.", e["corpo"]),
        ModeloDados(), Spacer(1, 0.14 * cm),
        p("Regras de integridade referencial", e["subtitulo"]),
        tabela([
            ["Regra", "Garantia implementada"],
            ["FK válida", "O serviço aceita -1 para voo não associado ou um ID positivo existente no arquivo de clientes."],
            ["Cliente com voos", "A interface bloqueia a exclusão do cliente quando a B+ indica voos associados."],
            ["Busca dos associados", "A consulta por cliente usa a B+ para obter todos os IDs e confirma a FK no voo recuperado."],
            ["Atualização da associação", "O ID deixa a chave anterior e passa a compor a nova chave da B+."],
        ], [5.1 * cm, 11.1 * cm], e),
        p("Decisão 1", e["subtitulo"]),
        p("O uso de idCliente diretamente em Voo materializa o lado N do relacionamento e simplifica a persistência. A regra também é aplicada dentro de AgenciaService; dessa forma, um chamador que não use a interface não consegue criar um voo com uma FK inexistente.", e["corpo"]),
        PageBreak(),
        p("2. Arquitetura proposta", e["titulo"]), Linha(), Spacer(1, 0.28 * cm),
        p("A solução foi organizada por responsabilidades. As telas apresentam e validam os dados. Os serviços coordenam o CRUD de voos, a integridade do relacionamento e a recuperação de índice. O componente genérico Arquivo cuida da persistência binária e do Hash; a Árvore B+ atende especificamente a consulta pelo cliente.", e["corpo"]),
        Arquitetura(),
        p("Responsabilidades principais", e["subtitulo"]),
        tabela([
            ["Componente", "Responsabilidade"],
            ["Principal", "Inicializa a aplicação e conecta as telas no mesmo fluxo de navegação."],
            ["TelaClientes e TelaVoos", "Permitem CRUD, seleção de cliente, consulta de voos, associação e ordenação."],
            ["AgenciaService", "Coordena voos, valida FK, consulta B+ e recupera o índice após falhas."],
            ["Arquivo", "Grava registros binários, mantém espaços livres e atualiza o Hash da PK."],
            ["HashExtensivel e ArvoreBMais", "Persistem os índices de PK e de FK em arquivos secundários."],
        ], [5.1 * cm, 11.1 * cm], e),
        PageBreak(),
    ]

    # Hash
    s += [
        p("3. Hash Extensível persistente", e["titulo"]), Linha(), Spacer(1, 0.28 * cm),
        p("O Hash Extensível é a estrutura de acesso direto utilizada por Arquivo. Ele mapeia a chave primária para o endereço físico em que o registro está gravado. Como o índice é persistido, a busca por ID continua disponível depois que a aplicação é fechada e reaberta.", e["corpo"]),
        TresBlocos(
            ["DIRETÓRIO", "BUCKETS", "ARQUIVO DE DADOS"],
            [["profundidade global", "ponteiros para buckets"], ["profundidade local", "PK -> endereço físico"], ["cabeçalho e registros", "ativos ou excluídos"]],
            "Busca direta: a PK seleciona o bucket, e o bucket aponta para o endereço físico do registro.",
        ),
        p("Arquivos e atualização", e["subtitulo"]),
        tabela([
            ["Arquivo ou operação", "Conteúdo e comportamento"],
            ["*.hash.dir.db", "Profundidade global e diretório de ponteiros para buckets."],
            ["*.hash.buckets.db", "Profundidade local e pares PK -> endereço físico."],
            ["Inclusão", "A nova PK e o endereço físico são inseridos; buckets se dividem quando necessário."],
            ["Atualização e exclusão", "O endereço é atualizado quando o registro muda; exclusões removem a chave do índice."],
        ], [5.6 * cm, 10.6 * cm], e),
        p("Decisão 2 - recuperação do Hash", e["subtitulo"]),
        p("Na abertura, o Hash é carregado dos seus arquivos binários. Uma conferência compara os registros ativos e seus endereços com o índice; a reconstrução por varredura ocorre apenas se os arquivos estiverem ausentes ou se houver inconsistência. Assim, a persistência é usada de fato e há uma rota segura de recuperação.", e["corpo"]),
        PageBreak(),
    ]

    # B+ e ordenação
    s += [
        p("4. Árvore B+ e consulta pela FK", e["titulo"]), Linha(), Spacer(1, 0.28 * cm),
        p("A Árvore B+ secundária é mantida no arquivo voos.id_cliente.bplus.db. As chaves ficam nas folhas e cada chave idCliente aponta para uma lista ordenada dos IDs de voo. Isso permite recuperar vários registros para a mesma FK sem percorrer todo o arquivo principal.", e["corpo"]),
        TresBlocos(
            ["RAIZ DA B+", "FOLHA DA FK", "VOOS ASSOCIADOS"],
            [["separadores de chaves", "direcionam a busca"], ["idCliente 7", "idCliente 9", "idCliente 12"], ["7 -> [16, 21]", "9 -> [17]", "12 -> [22, 24]"]],
            "As folhas mantêm a FK e todos os IDs associados; cada ID é recuperado no arquivo de voos pelo Hash.",
        ),
        p("Caminho da consulta 1:N", e["subtitulo"]),
        tabela([
            ["Etapa", "Processamento"],
            ["1. Informar cliente", "A tela recebe o ID do cliente ou seleciona uma linha da lista de clientes cadastrados."],
            ["2. Consultar B+", "A busca localiza a folha de idCliente e devolve todos os IDs de voo daquela chave."],
            ["3. Ler os voos", "Cada ID retornado é lido pelo Hash Extensível do arquivo de voos."],
            ["4. Exibir resultado", "A interface apresenta os voos associados no painel Voos do Cliente."],
        ], [5.1 * cm, 11.1 * cm], e),
        p("Decisões 3 e 4", e["subtitulo"]),
        p("Uma FK repetida acrescenta outro ID na lista de valores da mesma chave, sem duplicar o cliente. A serialização armazena nós, filhos, valores das folhas e seu encadeamento. Ao reiniciar, a estrutura é recarregada e conferida com os voos ativos.", e["corpo"]),
        PageBreak(),
        p("5. Ordenação externa por intercalação", e["titulo"]), Linha(), Spacer(1, 0.28 * cm),
        p("A ordenação externa usa a chave composta data/hora do voo e código como desempate. Em vez de carregar todos os registros em memória principal, a rotina gera pequenos runs ordenados em disco e os intercala dois a dois até produzir um único arquivo final.", e["corpo"]),
        TresBlocos(
            ["ARQUIVO PRINCIPAL", "RUNS E INTERCALAÇÃO", "ARQUIVO FINAL"],
            [["registros ativos", "mantido sem alteração"], ["blocos de até 3 voos", "merge de dois runs"], ["data/hora + código", "resultado binário ordenado"]],
            "O resultado é voos_ordenados_por_data_hora.db; os arquivos temporários são removidos após a intercalação.",
        ),
        p("Decisão 5 - runs e intercalação", e["subtitulo"]),
        p("O bloco de trabalho contém até três voos. Cada bloco é ordenado em memória e gravado como um run temporário. Nas passagens seguintes, dois runs são mesclados em outro arquivo temporário. Ao final, todos os voos ativos aparecem no arquivo de saída, em ordem cronológica, sem alterar a ordem física do arquivo principal.", e["corpo"]),
        tabela([
            ["Entrada", "Saída"],
            ["Arquivo de voos com registros ativos", "Arquivo binário ordenado por data/hora, com código como desempate."],
            ["Comparador: data/hora e código", "Runs temporários e intercalações realizados em memória secundária."],
        ], [8.1 * cm, 8.1 * cm], e),
        PageBreak(),
    ]

    # Integridade e decisões finais
    s += [
        p("6. Sincronização, recuperação e decisões", e["titulo"]), Linha(), Spacer(1, 0.28 * cm),
        p("A manutenção dos índices acompanha os dados em todas as operações. Por esse motivo, as estruturas são atualizadas no fluxo de inclusão, alteração e exclusão. Há verificações na abertura e uma recuperação da B+ a partir do arquivo de voos quando alguma etapa do CRUD falha.", e["corpo"]),
        tabela([
            ["Operação", "Dados e índices"],
            ["Criar voo", "Arquivo grava o voo e atualiza o Hash; AgenciaService inclui o ID sob a FK na B+."],
            ["Alterar voo", "Arquivo mantém a PK no Hash; a B+ remove a associação anterior e registra a nova FK."],
            ["Excluir voo", "Arquivo aplica lápide e remove a PK do Hash; a B+ remove o ID da lista da FK."],
            ["Falha no fluxo", "O serviço tenta reconstruir a B+ a partir dos voos ativos para restabelecer a correspondência."],
            ["Reabertura", "Hash e B+ são carregados; se a conferência detectar divergência, o índice é reconstruído."],
        ], [5.1 * cm, 11.1 * cm], e),
        p("Decisões 6 e 7", e["subtitulo"]),
        p("A maior mudança arquitetural foi substituir o vínculo N:N legado por idCliente em Voo. Arquivo recebeu a responsabilidade de manter o Hash por PK, ArvoreBMais passou a persistir o índice de FK e AgenciaService concentra a coordenação entre os dois. Essa divisão permite demonstrar cada estrutura e reduz o acoplamento da interface com a persistência.", e["corpo"]),
        p("<b>Persistência de arquivos.</b> Os dados e índices ficam no diretório dados. O README do repositório descreve a compilação com JDK 21, os caminhos de cada arquivo persistido e o roteiro de demonstração. Dados de execução não são versionados; o repositório contém o código, a documentação e as instruções necessárias para recriar a base local.", e["nota"]),
    ]

    documento = SimpleDocTemplate(
        str(OUTPUT), pagesize=A4, leftMargin=2.0 * cm, rightMargin=2.0 * cm,
        topMargin=1.72 * cm, bottomMargin=1.55 * cm,
        title="Agência de Voos - Trabalho Prático - Fase II",
        author="Luiz Henrique de Paula Diniz e Eder Arthur",
        subject="Decisões de projeto, índices persistentes e ordenação externa",
    )
    documento.build(s, onFirstPage=rodape_capa, onLaterPages=cabecalho)
    print(OUTPUT)


if __name__ == "__main__":
    gerar()
