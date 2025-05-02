import java.io.*;
import java.util.*;

public class ElMagoDeLasPalabras {

    private static final Scanner sc = new Scanner(System.in);
    private static final String VOCALES = "aeiouáéíóú";
    private static final String VOCALES_SIN_TILDES = "aeiou";
    private static final Random rand = new Random();
    private static final int RONDAS = 3;
    private static final String LETRAS = "abcdefghijklmnñopqrstuvwxyz";
    private static final String LETRAS_CON_TILDES = "abcdefghijklmnñopqrstuvwxyzáéíóú";

    public static void main(String[] args) throws IOException {
        System.out.println("Bienvenido a El Mago de las Palabras ✨\n");
        System.out.println("Reglas del juego:");
        System.out.println("- Modalidad REGULAR: letras se pueden repetir, no aparecen tildes. Al menos una vocal está garantizada. Se permite ingresar palabras con o sin acentos.");
        System.out.println("- Modalidad EXPERTA: letras no se repiten, incluyen tildes, y las palabras deben tener al menos 4 letras. Al menos una vocal está garantizada.");
        System.out.println("  También se permiten ingresar vocales sin acento en lugar de las vocales acentuadas.\n");

        String nombreArchivo = "C:\\Users\\johab\\Downloads\\palabrasParaMago.txt";

        int numJugadores;
        do {
            System.out.print("Ingresa el número de jugadores (2 a 4): ");
            numJugadores = sc.nextInt();
            sc.nextLine();
        } while (numJugadores < 2 || numJugadores > 4);

        Map<String, Integer> puntajes = new HashMap<>();
        Map<String, List<String>> palabrasPorJugador = new HashMap<>();
        Map<String, List<String>> perdidasPorJugador = new HashMap<>();
        Set<String> diccionario = cargarDiccionarioDesdeArchivo(nombreArchivo);

        List<String> jugadores = new ArrayList<>();
        for (int i = 1; i <= numJugadores; i++) {
            System.out.print("Nombre del jugador " + i + ": ");
            String nombre = sc.nextLine();
            jugadores.add(nombre);
            puntajes.put(nombre, 0);
            palabrasPorJugador.put(nombre, new ArrayList<>());
            perdidasPorJugador.put(nombre, new ArrayList<>());
        }

        System.out.print("Selecciona modalidad (1 = Regular, 2 = Experto): ");
        int modo = sc.nextInt();
        sc.nextLine();

        jugarRondas(jugadores, puntajes, palabrasPorJugador, perdidasPorJugador, diccionario, modo, RONDAS);

        while (true) {
            List<String> empatados = jugadoresEmpatados(jugadores, puntajes);
            if (empatados.size() <= 1) break;
            System.out.println("\n⚔️ ¡Empate! Se jugará una ronda de desempate entre los empatados.");
            jugarRondas(empatados, puntajes, palabrasPorJugador, perdidasPorJugador, diccionario, modo, 1);
        }

        String ganador = Collections.max(jugadores, Comparator.comparingInt(puntajes::get));
        System.out.println("\n🎉 El ganador es: " + ganador + " con " + puntajes.get(ganador) + " puntos!");
    }

    private static void jugarRondas(List<String> jugadores, Map<String, Integer> puntajes, Map<String, List<String>> palabrasPorJugador,
                                    Map<String, List<String>> perdidasPorJugador, Set<String> diccionario, int modo, int rondas) {
        for (int ronda = 1; ronda <= rondas; ronda++) {
            System.out.println("\n-- Ronda " + ronda + " --");
            List<Character> letrasRonda = generarLetras(10, modo);
            System.out.println("Letras disponibles: " + letrasRonda);

            Set<String> palabrasUsadas = new HashSet<>();
            Map<String, Boolean> quiereSeguir = new HashMap<>();
            jugadores.forEach(j -> quiereSeguir.put(j, true));


            Iterator<String> itInicio = jugadores.iterator(); //i
            while (itInicio.hasNext()) {
                String jugador = itInicio.next();
                procesarPalabra(jugador, letrasRonda, modo, diccionario, palabrasUsadas, puntajes, palabrasPorJugador, perdidasPorJugador);
            }

            while (quiereSeguir.containsValue(true)) {
                Iterator<String> it = jugadores.iterator();
                while (it.hasNext()) {
                    String jugador = it.next();
                    if (!quiereSeguir.get(jugador)) continue;

                    System.out.print(jugador + ", ¿quieres escribir otra palabra? (s/n): ");
                    String respuesta = sc.nextLine().trim().toLowerCase();
                    if (respuesta.equals("s")) {
                        procesarPalabra(jugador, letrasRonda, modo, diccionario, palabrasUsadas, puntajes, palabrasPorJugador, perdidasPorJugador);
                    } else {
                        quiereSeguir.put(jugador, false);
                    }
                }
            }

            mostrarPuntajes(jugadores, puntajes, palabrasPorJugador, perdidasPorJugador);
        }
    }

    private static void procesarPalabra(String jugador, List<Character> letrasRonda, int modo, Set<String> diccionario,
                                        Set<String> palabrasUsadas, Map<String, Integer> puntajes,
                                        Map<String, List<String>> palabrasPorJugador, Map<String, List<String>> perdidasPorJugador) {

        System.out.print(jugador + ", escribe una palabra: ");
        String palabra = sc.nextLine().toLowerCase();

        String palabraNormalizada = quitarTildes(palabra);
        String palabraVerificable = modo == 1 ? palabraNormalizada : palabra;

        if (modo == 2 && palabra.length() < 4) {
            System.out.println("Palabra demasiado corta para modo experto.");
            puntajes.put(jugador, puntajes.get(jugador) - 5);
            perdidasPorJugador.get(jugador).add(palabra + " (-5 pts)");
        } else if (!palabraValida(palabraVerificable, letrasRonda, modo)) {
            System.out.println("Palabra inválida: contiene letras no disponibles.");
            puntajes.put(jugador, puntajes.get(jugador) - 5);
            perdidasPorJugador.get(jugador).add(palabra + " (-5 pts)");
        } else if (!diccionario.contains(palabra) && !diccionario.contains(palabraNormalizada)) {
            System.out.println("Palabra inválida: no está en el diccionario.");
            puntajes.put(jugador, puntajes.get(jugador) - 5);
            perdidasPorJugador.get(jugador).add(palabra + " (-5 pts)");
        } else if (palabrasUsadas.contains(palabra)) {
            System.out.println("Palabra ya usada en esta ronda.");
            puntajes.put(jugador, puntajes.get(jugador) - 5);
            perdidasPorJugador.get(jugador).add(palabra + " (-5 pts)");
        } else {
            int puntos = calcularPuntuacion(palabra);
            puntajes.put(jugador, puntajes.get(jugador) + puntos);
            palabrasPorJugador.get(jugador).add(palabra + " (" + puntos + " pts)");
            palabrasUsadas.add(palabra);
            System.out.println("Palabra válida! +" + puntos + " puntos.");
        }
    }

    private static List<String> jugadoresEmpatados(List<String> jugadores, Map<String, Integer> puntajes) {
        int maxPuntos = jugadores.stream().mapToInt(puntajes::get).max().orElse(0);
        List<String> empatados = new ArrayList<>();
        for (String jugador : jugadores) {
            if (puntajes.get(jugador) == maxPuntos) {
                empatados.add(jugador);
            }
        }
        return empatados;
    }

    private static List<Character> generarLetras(int cantidad, int modo) {
        Set<Character> letrasGeneradas = new LinkedHashSet<>();
        List<Character> todas = new ArrayList<>();
        String fuente = modo == 2 ? LETRAS_CON_TILDES : LETRAS;
        for (char c : fuente.toCharArray()) todas.add(c);

        char vocal = (modo == 2 ? VOCALES : VOCALES_SIN_TILDES).charAt(rand.nextInt(modo == 2 ? VOCALES.length() : VOCALES_SIN_TILDES.length()));
        letrasGeneradas.add(vocal);

        while (letrasGeneradas.size() < cantidad) {
            char letra = todas.get(rand.nextInt(todas.size()));
            letrasGeneradas.add(letra);
        }
        return new ArrayList<>(letrasGeneradas);
    }

    private static int calcularPuntuacion(String palabra) {
        return (int) palabra.chars().map(c -> VOCALES.indexOf(c) >= 0 ? 5 : 3).sum();
    }

    private static boolean palabraValida(String palabra, List<Character> letras, int modo) {
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

    private static String quitarTildes(String palabra) {
        return palabra.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u');
    }

    private static void mostrarPuntajes(List<String> jugadores, Map<String, Integer> puntajes, Map<String, List<String>> palabrasPorJugador, Map<String, List<String>> perdidasPorJugador) {
        System.out.println("\n--- Puntajes actuales ---");
        jugadores.forEach(jugador -> {
            System.out.println(jugador + ": " + puntajes.get(jugador) + " puntos");
            System.out.println("Palabras válidas: " + palabrasPorJugador.get(jugador));
            System.out.println("Palabras inválidas: " + perdidasPorJugador.get(jugador));
        });
    }

    private static Set<String> cargarDiccionarioDesdeArchivo(String ruta) throws IOException {
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
