import java.util.List;
import java.util.Scanner;

public class MenuClientes {

    private ClienteDAO clienteDAO;
    private Scanner scanner;

    public MenuClientes(Scanner scanner) throws Exception {
        clienteDAO = new ClienteDAO();
        this.scanner = scanner;
    }

    public void menu() {

        int opcao;

        do {

            System.out.println("\n==============================");
            System.out.println("       GERENCIAR CLIENTES");
            System.out.println("==============================");

            System.out.println("1 - Buscar cliente");
            System.out.println("2 - Cadastrar cliente");
            System.out.println("3 - Alterar cliente");
            System.out.println("4 - Excluir cliente");
            System.out.println("5 - Listar todos os clientes");
            System.out.println("0 - Voltar");

            System.out.print("Opção: ");

            try {
                opcao = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {

                case 1:
                    buscarCliente();
                    break;

                case 2:
                    cadastrarCliente();
                    break;

                case 3:
                    alterarCliente();
                    break;

                case 4:
                    excluirCliente();
                    break;

                case 5:
                    listarClientes();
                    break;

                case 0:
                    System.out.println("Voltando...");
                    break;

                default:
                    System.out.println("Opção inválida.");
            }

        } while (opcao != 0);

    }

    private void buscarCliente() {

        System.out.print("\nID do cliente: ");

        int id;

        try {
            id = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {

            Cliente cliente =
                    clienteDAO.buscarCliente(id);

            if (cliente != null) {
                System.out.println(cliente);
            } else {
                System.out.println(
                    "Cliente não encontrado."
                );
            }

        } catch (Exception e) {

            System.out.println(
                "Erro ao buscar cliente."
            );
        }
    }

    private void cadastrarCliente() {

        System.out.println("\n===== CADASTRO DE CLIENTE =====");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        String cpf = lerApenasDigitos("CPF (somente números): ", "CPF", 11);

        String telefone = lerApenasDigitosTelefone("Telefone (somente números): ");

        System.out.print("E-mail: ");
        String email = scanner.nextLine();

        Cliente cliente =
                new Cliente(
                    nome,
                    cpf,
                    telefone,
                    email
                );

        try {

            if (clienteDAO.incluirCliente(cliente)) {

                System.out.println(
                    "\nCliente cadastrado com sucesso!"
                );

                System.out.println(
                    "ID gerado: " +
                    cliente.getId()
                );

            } else {

                System.out.println(
                    "Erro ao cadastrar cliente."
                );
            }

        } catch (Exception e) {

            System.out.println(
                "Erro ao cadastrar cliente."
            );

            e.printStackTrace();
        }
    }

    private void alterarCliente() {

        System.out.print(
            "\nID do cliente: "
        );

        int id;

        try {
            id = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {

            Cliente cliente =
                    clienteDAO.buscarCliente(id);

            if (cliente == null) {

                System.out.println(
                    "Cliente não encontrado."
                );

                return;
            }

            System.out.println(
                "\nCliente atual:"
            );

            System.out.println(cliente);

            System.out.print(
                "\nNovo nome: "
            );

            String nome =
                    scanner.nextLine();

            String cpf = lerApenasDigitos("Novo CPF (somente números): ", "CPF", 11);

            String telefone = lerApenasDigitosTelefone("Novo telefone (somente números): ");

            System.out.print(
                "Novo e-mail: "
            );

            String email =
                    scanner.nextLine();

            cliente.setNome(nome);
            cliente.setCpf(cpf);
            cliente.setTelefone(telefone);
            cliente.setEmail(email);

            if (clienteDAO.alterarCliente(cliente)) {

                System.out.println(
                    "Cliente alterado com sucesso!"
                );

            } else {

                System.out.println(
                    "Não foi possível alterar."
                );
            }

        } catch (Exception e) {

            System.out.println(
                "Erro ao alterar cliente."
            );

            e.printStackTrace();
        }
    }

    private void excluirCliente() {

        System.out.print(
            "\nID do cliente: "
        );

        int id;

        try {
            id = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {

            Cliente cliente =
                    clienteDAO.buscarCliente(id);

            if (cliente == null) {

                System.out.println(
                    "Cliente não encontrado."
                );

                return;
            }

            System.out.println(cliente);

            System.out.print(
                "\nConfirmar exclusão? (S/N): "
            );

            String resposta =
                    scanner.nextLine();

            if (resposta.equalsIgnoreCase("S")) {

                if (clienteDAO.excluirCliente(id)) {

                    System.out.println(
                        "Cliente excluído com sucesso!"
                    );

                } else {

                    System.out.println(
                        "Erro ao excluir cliente."
                    );
                }

            } else {

                System.out.println(
                    "Operação cancelada."
                );
            }

        } catch (Exception e) {

            System.out.println(
                "Erro ao excluir cliente."
            );

            e.printStackTrace();
        }
    }
    private void listarClientes() {
        try {
            List<Cliente> clientes = clienteDAO.listarClientes();

            System.out.println("\n===== TODOS OS CLIENTES =====");

            if (clientes.isEmpty()) {
                System.out.println("Nenhum cliente cadastrado.");
                return;
            }

            for (Cliente cliente : clientes) {
                System.out.println("------------------------------");
                System.out.println(cliente);
            }
            System.out.println("------------------------------");
            System.out.println("Total de clientes: " + clientes.size());

        } catch (Exception e) {
            System.out.println("Erro ao listar clientes.");
            e.printStackTrace();
        }
    }

    private String lerApenasDigitos(String mensagem, String campo, int quantidadeExata) {
        while (true) {
            System.out.print(mensagem);
            String valor = scanner.nextLine().trim();

            if (!valor.matches("\\d+")) {
                System.out.println(campo + " deve conter somente números.");
                continue;
            }

            if (valor.length() != quantidadeExata) {
                System.out.println(campo + " deve conter exatamente " + quantidadeExata + " dígitos.");
                continue;
            }

            return valor;
        }
    }

    private String lerApenasDigitosTelefone(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String valor = scanner.nextLine().trim();

            if (!valor.matches("\\d+")) {
                System.out.println("Telefone deve conter somente números.");
                continue;
            }

            if (valor.length() != 10 && valor.length() != 11) {
                System.out.println("Telefone deve conter 10 ou 11 dígitos.");
                continue;
            }

            return valor;
        }
    }

}