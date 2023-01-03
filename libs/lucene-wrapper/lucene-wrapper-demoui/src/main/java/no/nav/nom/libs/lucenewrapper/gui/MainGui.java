package no.nav.nom.libs.lucenewrapper.gui;


import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

public class MainGui implements Runnable {

    private final JPanel mainPane = new JPanel();
    private final JLabel label = new JLabel("Search here");
    private final JTextField field = new JTextField("");
    private final JTextArea console = new JTextArea("Waiting for search terms");
    private JFrame frame;


    public MainGui() {
        console.setEditable(false);
        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.2;
        gbc.fill = GridBagConstraints.BOTH;
        mainPane.setLayout(new GridBagLayout());
        mainPane.add(label, gbc);
        gbc.gridy = 1;
        mainPane.add(field, gbc);
        gbc.gridy = 2;
        gbc.weighty = 10.0;
        var scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setAutoscrolls(false);
        scrollPane.setViewportView(console);
        mainPane.add(scrollPane, gbc);
    }



    private void setupEventListeners(WrappedLucene repo) {
        this.field.getDocument().addDocumentListener(new DocumentListener() {

            private void react() {
                var newTxt = field.getText();
                var res = repo.search(newTxt);
                var sb = new StringBuilder();
                sb.append("Hits: ").append(res.size()).append("\n");
                for (var x : res) {
                    var fieldsToDisplay = java.util.List.of(x.id(), x.name(), x.entityType(), String.valueOf(x.score()));
                    var strToDisplay = fieldsToDisplay.stream().map(it -> "[ " + it + " ]").collect(Collectors.joining(",   "));
                    sb.append(strToDisplay).append("\n");
                }
                console.setText(sb.toString());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                react();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                react();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });
    }

    private void switchToSearchGui(){
        this.frame.setContentPane(this.mainPane);
        // following lines are necessary to trigger a repaint
        this.frame.setVisible(false);
        this.frame.invalidate();
        this.frame.repaint();
        this.frame.setVisible(true);
    }


    @Override
    public void run() {
        this.frame = new JFrame();
        frame.setTitle("Lucene wrapper demo-ui");
        frame.setMinimumSize(new Dimension(800, 800));
        JPanel temp = new JPanel();
        temp.add(new JLabel("Loading from files..."));
        frame.setContentPane(temp);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }


    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        MainGui mg = new MainGui();
        SwingUtilities.invokeAndWait(mg);
        mg.setupEventListeners(GuiSearchRepo.guiSearchRepo());
        mg.switchToSearchGui();
    }
}
