import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class ElMagoDeLasPalabrasGui {
    // Constantes para letras y configuración del juego
    private static final String VOCALES = "aeiouáéíóú";
    private static final String VOCALES_SIN_TILDES = "aeiou";
    private static final String LETRAS = "abcdefghijklmnñopqrstuvwxyz";
    private static final String LETRAS_CON_TILDES = "abcdefghijklmnñopqrstuvwxyzáéíóú";
    private static final int RONDAS_TOTALES = 3;

    // Componentes de la interfaz gráfica
    private JFrame frame;
    private JPanel panelPrincipal, panelJuego;
    private JTextArea letrasArea;
    private JTextField palabraField;
    private JTextArea mensajeArea;
    private JButton registrarButton, pasarButton, agregarButton;

    // Estructuras de datos para el juego
    private List<String> jugadores;
    private Map<String, Integer> puntajes;
    private Map<String, List<String>> palabrasPorJugador;
    private Map<String, List<String>> perdidasPorJugador;
    private Set<String> diccionario;
    private Set<String> palabrasUsadasRonda;
    private List<Character> letrasRonda;

    // Control de turnos y rondas
    private int turnoActual;
    private int rondaActual;
    private int modo;
    private Map<String, Boolean> quiereSeguir;
    private String archivoDiccionario = "C:\\Users\\V16\\Downloads\\palabrasParaMago.txt";

    public static void main(String[] args) {
        // Inicia el juego en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> new ElMagoDeLasPalabrasGui().crearVentanaInicio());
    }

    private void crearVentanaInicio() {
        // Crea ventana principal
        frame = new JFrame("El Mago de las Palabras ✨");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panelPrincipal = new JPanel(new BorderLayout());

        // Título superior
        JLabel titulo = new JLabel("El Mago de las Palabras ✨", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.BOLD, 24));
        panelPrincipal.add(titulo, BorderLayout.NORTH);

        // Imagen central
        ImageIcon icono = new ImageIcon("C:\\Users\\V16\\Downloads\\magoDelasPalbras.jpeg");
        Image imagenEscalada = icono.getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH);
        JLabel imagenLabel = new JLabel(new ImageIcon(imagenEscalada), SwingConstants.CENTER);
        panelPrincipal.add(imagenLabel, BorderLayout.CENTER);

        // Botones inferiores
        JPanel botones = new JPanel();
        JButton jugarBtn = new JButton("Jugar");
        JButton salirBtn = new JButton("Salir");
        botones.add(jugarBtn);
        botones.add(salirBtn);
        panelPrincipal.add(botones, BorderLayout.SOUTH);

        // Acciones de los botones
        jugarBtn.addActionListener(e -> iniciarConfiguracion());
        salirBtn.addActionListener(e -> System.exit(0));

        // Mostrar ventana
        frame.getContentPane().add(panelPrincipal);
        frame.setSize(500, 400);
        frame.setVisible(true);
    }

    private void iniciarConfiguracion() {
        // Inicializa estructuras
        jugadores = new ArrayList<>();
        puntajes = new HashMap<>();
        palabrasPorJugador = new HashMap<>();
        perdidasPorJugador = new HashMap<>();

        // Pide número y nombres de jugadores
        int numJugadores = Integer.parseInt(JOptionPane.showInputDialog(frame, "¿Cuántos jugadores? (2 a 4)"));
        for (int i = 1; i <= numJugadores; i++) {
            String nombre = JOptionPane.showInputDialog(frame, "Nombre del jugador " + i + ":");
            jugadores.add(nombre);
            puntajes.put(nombre, 0);
            palabrasPorJugador.put(nombre, new ArrayList<>());
            perdidasPorJugador.put(nombre, new ArrayList<>());
        }

        // Selección de dificultad
        String[] opciones = {"Regular", "Experto"};
        int seleccion = JOptionPane.showOptionDialog(frame, "Selecciona la dificultad:", "Modo de juego",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
        modo = seleccion + 1;

        // Cargar diccionario desde archivo
        try {
            diccionario = cargarDiccionarioDesdeArchivo(archivoDiccionario);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error al cargar el diccionario.");
            System.exit(1);
        }

        iniciarJuego();
    }

    private void iniciarJuego() {
        // Panel principal del juego
        panelJuego = new JPanel(new BorderLayout());

        // Área superior con letras
        letrasArea = new JTextArea();
        letrasArea.setFont(new Font("Monospaced", Font.BOLD, 18));
        letrasArea.setEditable(false);
        panelJuego.add(letrasArea, BorderLayout.NORTH);

        // Área central con entrada de palabra y mensajes
        JPanel centro = new JPanel(new BorderLayout());
        palabraField = new JTextField();
        centro.add(palabraField, BorderLayout.NORTH);

        mensajeArea = new JTextArea(10, 40);
        mensajeArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(mensajeArea);
        centro.add(scroll, BorderLayout.CENTER);

        panelJuego.add(centro, BorderLayout.CENTER);

        // Botones de control del juego
        JPanel botones = new JPanel();
        registrarButton = new JButton("Registrar palabra");
        pasarButton = new JButton("Pasar turno");
        agregarButton = new JButton("Agregar al diccionario");
        botones.add(registrarButton);
        botones.add(pasarButton);
        botones.add(agregarButton);

        panelJuego.add(botones, BorderLayout.SOUTH);

        // Reemplaza el contenido del frame por el panel de juego
        frame.setContentPane(panelJuego);
        frame.revalidate();
        frame.repaint();

        rondaActual = 1;
        turnoActual = 0;
        quiereSeguir = new HashMap<>();

        // Todos los jugadores quieren jugar inicialmente
        for (String j : jugadores) quiereSeguir.put(j, true);
        iniciarRonda();

        // Listeners de botones
        registrarButton.addActionListener(e -> procesarPalabra());
        pasarButton.addActionListener(e -> pasarTurno());
        agregarButton.addActionListener(e -> agregarAlDiccionario());
    }

    private void iniciarRonda() {
        // Prepara la ronda y genera letras
        mensajeArea.setText("\n-- Ronda " + rondaActual + " -- Turno de: " + jugadores.get(turnoActual) + " --\n");
        letrasRonda = generarLetras(10, modo);
        letrasArea.setText("Letras disponibles: " + letrasRonda + "\n");
        palabrasUsadasRonda = new HashSet<>();
        for (String j : jugadores) quiereSeguir.put(j, true);
    }

    private void procesarPalabra() {
        // Valida y puntúa la palabra del jugador actual
        String jugador = jugadores.get(turnoActual);
        String palabra = palabraField.getText().trim().toLowerCase();
        palabraField.setText("");

        if (!quiereSeguir.get(jugador)) return;

        String palabraNormalizada = quitarTildes(palabra);
        String palabraVerificable = modo == 1 ? palabraNormalizada : palabra;

        if (modo == 2 && palabra.length() < 4) {
            mensajeArea.append("Palabra demasiado corta para modo experto (-5 pts).\n");
            actualizarPuntaje(jugador, palabra, -5, false);
        } else if (!palabraValida(palabraVerificable, letrasRonda, modo)) {
            mensajeArea.append("Palabra inválida: contiene letras no disponibles (-5 pts).\n");
            actualizarPuntaje(jugador, palabra, -5, false);
        } else if (!diccionario.contains(palabra) && !diccionario.contains(palabraNormalizada)) {
            int opcion = JOptionPane.showConfirmDialog(frame,
                    "La palabra \"" + palabra + "\" no está en el diccionario.\n¿Deseas agregarla?",
                    "Agregar palabra",
                    JOptionPane.YES_NO_OPTION);

            if (opcion == JOptionPane.YES_OPTION) {
                diccionario.add(palabra);
                mensajeArea.append("Palabra \"" + palabra + "\" agregada al diccionario y aceptada.\n");
                int puntos = calcularPuntuacion(palabra);
                actualizarPuntaje(jugador, palabra, puntos, true);
                palabrasUsadasRonda.add(palabra);
            } else {
                mensajeArea.append("Palabra inválida: no está en el diccionario (-5 pts).\n");
                actualizarPuntaje(jugador, palabra, -5, false);
            }
        } else if (palabrasUsadasRonda.contains(palabra)) {
            mensajeArea.append("Palabra ya usada en esta ronda (-5 pts).\n");
            actualizarPuntaje(jugador, palabra, -5, false);
        } else {
            int puntos = calcularPuntuacion(palabra);
            mensajeArea.append("Palabra válida! +" + puntos + " puntos.\n");
            actualizarPuntaje(jugador, palabra, puntos, true);
            palabrasUsadasRonda.add(palabra);
        }

        siguienteTurno();
    }

    private void pasarTurno() {
        // El jugador decide no participar en este turno
        String jugador = jugadores.get(turnoActual);
        quiereSeguir.put(jugador, false);
        mensajeArea.append(jugador + " ha pasado su turno.\n");
        siguienteTurno();
    }

    private void siguienteTurno() {
        // Avanza al siguiente jugador o a la siguiente ronda
        turnoActual = (turnoActual + 1) % jugadores.size();
        if (quiereSeguir.values().stream().noneMatch(v -> v)) {
            rondaActual++;
            if (rondaActual > RONDAS_TOTALES) {
                finDelJuego();
            } else {
                iniciarRonda();
            }
        } else {
            mensajeArea.append("\n-- Turno de: " + jugadores.get(turnoActual) + " --\n");
        }
    }

    private void actualizarPuntaje(String jugador, String palabra, int puntos, boolean valida) {
        // Actualiza el puntaje y guarda la palabra como válida o inválida
        puntajes.put(jugador, puntajes.get(jugador) + puntos);
        if (valida) {
            palabrasPorJugador.get(jugador).add(palabra + " (" + puntos + " pts)");
        } else {
            perdidasPorJugador.get(jugador).add(palabra + " (" + puntos + " pts)");
        }
    }

    private void agregarAlDiccionario() {
        // Permite al jugador agregar una palabra manualmente
        String nuevaPalabra = palabraField.getText().trim().toLowerCase();
        palabraField.setText("");
        diccionario.add(nuevaPalabra);
        mensajeArea.append("Palabra agregada al diccionario: " + nuevaPalabra + "\n");
    }

    private void finDelJuego() {
        // Muestra los resultados finales
        mensajeArea.append("\n--- Fin del juego ---\n");
        for (String jugador : jugadores) {
            mensajeArea.append(jugador + ": " + puntajes.get(jugador) + " puntos\n");
            mensajeArea.append("Palabras válidas: " + palabrasPorJugador.get(jugador) + "\n");
            mensajeArea.append("Palabras inválidas: " + perdidasPorJugador.get(jugador) + "\n");
        }
    }

    private List<Character> generarLetras(int cantidad, int modo) {
        // Genera una lista de letras aleatorias con al menos una vocal
        Set<Character> letrasGeneradas = new LinkedHashSet<>();
        List<Character> todas = new ArrayList<>();
        String fuente = modo == 2 ? LETRAS_CON_TILDES : LETRAS;
        for (char c : fuente.toCharArray()) todas.add(c);

        Random rand = new Random();
        char vocal = (modo == 2 ? VOCALES : VOCALES_SIN_TILDES).charAt(rand.nextInt((modo == 2 ? VOCALES : VOCALES_SIN_TILDES).length()));
        letrasGeneradas.add(vocal);

        while (letrasGeneradas.size() < cantidad) {
            letrasGeneradas.add(todas.get(rand.nextInt(todas.size())));
        }
        return new ArrayList<>(letrasGeneradas);
    }

    private int calcularPuntuacion(String palabra) {
        // Calcula la puntuación según vocales (5 pts) y consonantes (3 pts)
        return (int) palabra.chars().map(c -> VOCALES.indexOf(c) >= 0 ? 5 : 3).sum();
    }

    private boolean palabraValida(String palabra, List<Character> letras, int modo) {
        // Verifica que la palabra esté compuesta solo por letras disponibles
        Map<Character, Integer> disponibles = new HashMap<>();

        if (modo == 1) {
            for (char c : palabra.toCharArray()) {
                if (!letras.contains(c)) return false;
            }
            return true;
        }

        for (char c : letras) disponibles.put(c, disponibles.getOrDefault(c, 0) + 1);
        for (char c : palabra.toCharArray()) {
            if (!disponibles.containsKey(c) || disponibles.get(c) == 0) return false;
            disponibles.put(c, disponibles.get(c) - 1);
        }
        return true;
    }

    private String quitarTildes(String palabra) {
        // Reemplaza vocales con tilde por su versión sin tilde
        return palabra.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u');
    }

    private Set<String> cargarDiccionarioDesdeArchivo(String ruta) throws IOException {
        // Lee el diccionario desde un archivo de texto
        Set<String> diccionario = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim().toLowerCase();
                if (!linea.isEmpty() && Character.isLetter(linea.charAt(0))) {
                    diccionario.add(linea);
                }
            }
        }
        return diccionario;
    }
}