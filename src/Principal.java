import java.util.Scanner;

public class Principal {

    public static void main(String[] args) {

        Scanner scanner =
                new Scanner(System.in);

        int opcao;

        try {

            do {

                System.out.println(
                    "\n=============================="
                );

                System.out.println(
                    "       AGENCIA DE VOOS"
                );

                System.out.println(
                    "=============================="
                );

                System.out.println(
                    "1 - Gerenciar clientes"
                );

                System.out.println(
                    "0 - Sair"
                );

                System.out.print(
                    "\nOpção: "
                );

                try {

                    opcao =
                        Integer.parseInt(
                            scanner.nextLine()
                        );

                } catch (NumberFormatException e) {

                    opcao = -1;
                }

                switch (opcao) {

                    case 1:

                        MenuClientes menuClientes =
                                new MenuClientes(scanner);

                        menuClientes.menu();

                        break;

                    case 0:

                        System.out.println(
                            "Encerrando sistema..."
                        );

                        break;

                    default:

                        System.out.println(
                            "Opção inválida."
                        );
                }

            } while (opcao != 0);

        } catch (Exception e) {

            System.out.println(
                "Erro fatal no sistema:"
            );

            e.printStackTrace();

        } finally {

            scanner.close();
        }
    }
}