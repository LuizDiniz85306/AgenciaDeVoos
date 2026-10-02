# Agência de Voos - AED III, Fase II

Aplicação Java Swing para cadastro de clientes e voos, com persistência em
arquivos binários e índices mantidos em memória secundária.

## Requisitos implementados

- CRUD de `Cliente` e `Voo` em arquivos `.db`, com exclusão lógica e
  reutilização segura de espaço.
- Relacionamento **1:N Cliente -> Voo**: `Voo.idCliente` é a chave estrangeira.
  Um cliente pode ter vários voos; um voo possui no máximo um cliente. A
  exclusão de um cliente com voos associados é bloqueada.
- Hash Extensível para a PK de cada arquivo de dados, reutilizado após
  reinicializar a aplicação e reconstruído somente se estiver ausente ou se a
  conferência com o arquivo de dados detectar divergência:
  - `dados/clientes/clientes.db.hash.dir.db`
  - `dados/clientes/clientes.db.hash.buckets.db`
  - `dados/voos/voos.db.hash.dir.db`
  - `dados/voos/voos.db.hash.buckets.db`
- Árvore B+ secundária persistida em
  `dados/voos/voos.id_cliente.bplus.db`, que mapeia `idCliente` para todos os
  IDs de voos associados. A estrutura também é reutilizada depois de reiniciar
  e é reconstruída somente como recuperação de consistência.
- Ordenação externa por intercalação balanceada de voos por data/hora. Ela
  gera blocos ordenados de até três registros, intercala os arquivos
  temporários em memória secundária e produz
  `dados/voos/voos_ordenados_por_data_hora.db`.
- Na primeira execução, associações ativas do arquivo legado `voos_clientes`
  são migradas para `Voo.idCliente`; como o modelo antigo era N:N, é mantida a
  primeira associação de cada voo. O arquivo original não é apagado.

As chamadas de serviço também validam a FK de cliente, portanto não é possível
criar um voo associado a um ID inexistente fora da interface. Se alguma etapa
do CRUD de voos falhar após alterar os dados, a Árvore B+ é reconstruída a
partir do arquivo binário para recuperar a sincronização.

## Como executar

O projeto não usa bibliotecas externas. Em um terminal com JDK 21:

```powershell
javac -encoding UTF-8 -d bin src\*.java
java -cp bin Principal
```

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
endereços dos buckets; cada bucket binário guarda pares `PK -> endereço físico`.
Ele é atualizado nas inclusões, atualizações e exclusões feitas por `Arquivo`.

A Árvore B+ mantém as chaves somente nas folhas juntamente com as listas de IDs
de voo. Os nós internos direcionam a busca até a folha. A serialização guarda
todos os nós, filhos e encadeamento das folhas, permitindo recuperar todas as
ocorrências de uma FK após reiniciar a aplicação.

A ordenação não carrega todos os voos em memória: gera runs ordenados e faz
intercalações de dois em dois até existir apenas o arquivo final.
