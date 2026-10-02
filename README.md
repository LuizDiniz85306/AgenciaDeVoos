# Agência de Voos - AED III, Fase II

Aplicação Java Swing para cadastro de clientes e voos, com dados e índices
persistidos em arquivos binários. O Hash Extensível e a Árvore B+ são
carregados em memória durante a execução.

## Requisitos implementados

- CRUD de `Cliente` e `Voo` em arquivos `.db`, com exclusão lógica e
  reutilização segura de espaço.
- Relacionamento **1:N Cliente -> Voo**: `Voo.idCliente` é a chave estrangeira.
  Um cliente pode ter vários voos; um voo possui no máximo um cliente. A
  interface bloqueia a exclusão de um cliente com voos associados. Chamadas
  diretas a `ClienteDAO.excluirCliente` não verificam esses vínculos.
- Hash Extensível para a PK de cada arquivo de dados, reutilizado após
  reinicializar a aplicação. Ele é reconstruído se estiver ausente, se o
  carregamento não for aprovado ou se a conferência com os dados detectar
  divergência. A recuperação pressupõe um arquivo de dados legível e não corrige
  toda forma de corrupção. Arquivos persistidos:
  - `dados/clientes/clientes.db.hash.dir.db`
  - `dados/clientes/clientes.db.hash.buckets.db`
  - `dados/voos/voos.db.hash.dir.db`
  - `dados/voos/voos.db.hash.buckets.db`
- Árvore B+ secundária persistida em
  `dados/voos/voos.id_cliente.bplus.db`, que mapeia `idCliente` para todos os
  IDs de voos associados. A estrutura é reutilizada depois de reiniciar quando
  consistente. Cada inserção ou remoção efetiva extrai as entradas, reconstrói
  toda a árvore em memória e regrava seu arquivo. Uma alteração de voo associado
  pode provocar duas reconstruções. A conferência na abertura compara os pares
  extraídos das folhas com os voos ativos, sem validar integralmente a estrutura.
- Ordenação externa por intercalação de duas vias de voos por data e horário. Ela
  gera blocos ordenados de até três registros, intercala os arquivos
  temporários em memória secundária e produz
  `dados/voos/voos_ordenados_por_data_hora.db`.
- Quando existe o arquivo legado `voos_clientes` e ainda não há marcador de
  migração, a primeira associação ativa com ID positivo de cada código de voo
  preenche `Voo.idCliente` nos voos sem FK positiva. O modelo antigo era N:N;
  o arquivo original não é apagado. A migração não revalida a existência do
  cliente associado.

Nas inclusões e alterações realizadas por `AgenciaService.salvarVoo`, a FK deve
ser `-1` (voo sem cliente) ou um ID positivo de cliente existente, inclusive em
chamadas sem a interface. Essa validação não é aplicada pela migração legada.

Em caso de exceção nas operações de salvar ou excluir voos, o serviço tenta
reconstruir a B+ a partir dos registros ativos. A recuperação também pode
falhar e não desfaz alterações já gravadas. Não há transação nem garantia de
gravação atômica entre dados, Hash e B+. O Hash é conferido novamente na abertura.

## Como executar

O projeto não usa bibliotecas externas. Use JDK 21 e execute os comandos da
raiz do projeto, onde estão as pastas `src` e `dados`.

Em um terminal PowerShell com `javac` e `java` disponíveis no PATH:

```powershell
$fontesAgencia = @(Get-ChildItem -LiteralPath .\src -Filter *.java -File | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 21 -d bin $fontesAgencia
if ($LASTEXITCODE -ne 0) { throw "A compilação falhou." }
java -cp bin Principal
```

Se o JDK não estiver no PATH, substitua `javac` e `java` pelos caminhos dos
executáveis da sua instalação. Por exemplo, com o JDK instalado neste caminho:

```powershell
$fontesAgencia = @(Get-ChildItem -LiteralPath .\src -Filter *.java -File | ForEach-Object { $_.FullName })
& 'C:\Program Files\Java\jdk-21.0.12\bin\javac.exe' -encoding UTF-8 --release 21 -d bin $fontesAgencia
if ($LASTEXITCODE -ne 0) { throw "A compilação falhou." }
& 'C:\Program Files\Java\jdk-21.0.12\bin\java.exe' -cp bin Principal
```

A pasta `bin` não é versionada: recompile após clonar o repositório ou limpar
os arquivos compilados. O erro `ClassNotFoundException: Principal` pode ocorrer
se `bin/Principal.class` não tiver sido gerado ou se o classpath estiver errado.
No VS Code, abra a raiz do projeto, aguarde a compilação Java e execute
`Principal` com F5. Mantenha apenas uma instância da aplicação aberta por vez.

## Roteiro de demonstração

1. Cadastre dois clientes em **Gerenciar Clientes**.
2. Em **Gerenciar Voos**, cadastre três ou mais voos, selecionando o mesmo
   cliente no campo **Cliente (FK)** para pelo menos dois deles.
3. Após salvar um voo, seu ID aparece no campo **ID (PK)**. Limpe o código,
   informe somente o ID e use **Buscar**: esta operação usa o Hash Extensível.
4. Em **Gerenciar Clientes**, informe o ID de um cliente e clique em
   **Buscar voos**. A lista é recuperada pela Árvore B+ usando a FK.
5. Em **Gerenciar Voos**, clique em **Ordenar externamente**. O resultado
   permanece no arquivo binário ordenado sem alterar o arquivo de dados
   principal.
6. Feche e abra novamente a aplicação. Os arquivos de dados e de índices são
   recarregados do diretório `dados`; os arquivos dos índices não são
   regravados quando a conferência de consistência é aprovada.

## Entrega

O formulário de decisões de projeto da Fase II, incluindo o diagrama do
relacionamento e a explicação das estruturas persistidas, está em
[`output/pdf/Fase II - Decisoes de Projeto.pdf`](output/pdf/Fase%20II%20-%20Decisoes%20de%20Projeto.pdf).

## Decisões de projeto

O Hash Extensível armazena no diretório binário a profundidade global e os
identificadores dos buckets; cada bucket guarda sua profundidade local e pares
`PK -> endereço físico do registro`.
Ele é atualizado nas inclusões, atualizações e exclusões feitas por `Arquivo`.

As folhas da Árvore B+ armazenam as chaves `idCliente` e as listas ordenadas de
IDs de voos. Os nós internos guardam chaves separadoras que direcionam a busca
até a folha. A serialização guarda os nós, as referências aos filhos, os valores
e o encadeamento das folhas. A reconstrução completa após cada manutenção
simplifica a implementação, mas aumenta seu custo conforme a base cresce.

A ordenação não carrega todos os voos em memória: gera runs ordenados e faz
intercalações de dois em dois até produzir o arquivo final. Cada intercalação
mantém um registro corrente de cada entrada. O fluxo normal solicita a exclusão
dos temporários, mas não garante sua limpeza em caso de exceção ou falha na
exclusão de arquivos.
