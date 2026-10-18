import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

public final class Tema {
    public static final Color FUNDO = new Color(15, 23, 42);
    public static final Color PAINEL = new Color(30, 41, 59);
    public static final Color LINHA_ALTERNADA = new Color(38, 53, 74);
    public static final Color TEXTO = new Color(241, 245, 249);
    public static final Color TEXTO_SECUNDARIO = new Color(203, 213, 225);
    public static final Color BORDA = new Color(71, 85, 105);
    public static final Color PRIMARIA = new Color(37, 99, 235);
    public static final Color CABECALHO = new Color(30, 64, 175);
    public static final Color SELECAO = new Color(29, 78, 216);
    public static final Color SUCESSO = new Color(15, 118, 110);
    public static final Color PERIGO = new Color(185, 28, 28);
    public static final Color AVISO = new Color(180, 83, 9);
    public static final Color NEUTRA = new Color(71, 85, 105);
    public static final Color VOLTAR = new Color(203, 213, 225);
    public static final Color CAMPO = new Color(248, 250, 252);
    public static final Color TEXTO_CAMPO = new Color(15, 23, 42);
    public static final Color FOCO = new Color(147, 197, 253);

    private Tema() {}

    public static void aplicar() {
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("Button.background", VOLTAR);
        UIManager.put("Button.foreground", TEXTO_CAMPO);
        UIManager.put("TextField.background", CAMPO);
        UIManager.put("TextField.foreground", TEXTO_CAMPO);
        UIManager.put("ComboBox.foreground", TEXTO_CAMPO);
        UIManager.put("ComboBox.background", CAMPO);
        UIManager.put("List.background", CAMPO);
        UIManager.put("List.foreground", TEXTO_CAMPO);
        UIManager.put("List.selectionBackground", SELECAO);
        UIManager.put("List.selectionForeground", TEXTO);
        UIManager.put("TextArea.background", PAINEL);
        UIManager.put("TextArea.foreground", TEXTO);
        UIManager.put("Panel.background", PAINEL);
        UIManager.put("Viewport.background", PAINEL);
        UIManager.put("ScrollPane.background", PAINEL);
        UIManager.put("SplitPane.background", FUNDO);
        UIManager.put("TitledBorder.titleColor", TEXTO);
        UIManager.put("OptionPane.background", PAINEL);
        UIManager.put("OptionPane.foreground", TEXTO);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("OptionPane.buttonBackground", VOLTAR);
        UIManager.put("OptionPane.buttonForeground", TEXTO_CAMPO);
    }
}
