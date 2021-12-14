package br.com.AStar;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/*
    Universidade Estadual de Maringá - PR
    Ciência da Computação - Modelagem e Otimização Algorítmica
    Nome: Diogo Alves de Almeida
    RA: 95108

    CASOS
    "0 2 9 13 3 1 5 14 4 7 6 10 8 11 12 15";
    "3 2 1 9 0 5 6 13 4 7 10 14 8 12 15 11";
    "2 1 9 13 3 5 10 14 4 6 11 15 7 8 12 0";
    "9 13 10 0 5 2 6 14 1 7 11 15 3 4 8 12";
    "4 3 2 1 8 10 11 5 12 6 0 9 15 7 14 13";
    "9 13 14 15 5 6 10 8 0 1 11 12 7 2 3 4";
    "10 6 2 1 7 13 9 5 0 15 14 12 11 3 4 8";
    "6 2 1 5 4 10 13 9 0 8 3 7 12 15 11 14";
    "10 13 15 0 5 9 14 11 1 2 6 7 3 4 8 12";
    "5 9 13 14 1 6 7 10 11 15 12 0 8 2 3 4";
*/

public class Main {
    static int[] STATE_ACCEPTABLE = new int[]
            {
                    1, 5, 9, 13,
                    2, 6, 10, 14,
                    3, 7, 11, 15,
                    4, 8, 12, 0
            };

    public static void main(String[] args) {
        int heuristic = 5;
        Scanner scanner = new Scanner(System.in);
        String inputState = "10 13 15 0 5 9 14 11 1 2 6 7 3 4 8 12";
        //String inputState = scanner.nextLine();

        inputState = inputState.replaceAll("[^0-9]", " ");

        Object[] inputStateObject = Arrays
                .stream(inputState.split(" "))
                .filter(s -> s != null && s.length() > 0)
                .toArray();

        if (inputStateObject.length != 16) {
            System.out.println("Puzzle does not contain correct number of numbers");
            return;
        }

        // criando estado inicial
        StringBuilder initialStateBuilder = new StringBuilder();
        Node root = new Node();
        root.movement = "Start";
        for (int line = 0; line < 16; line++) {
            root.state[line] = Integer.parseInt(inputStateObject[line].toString());
            initialStateBuilder.append(root.state[line]).append(" ");
            if (root.state[line] == 0) {
                root.emptyIndex = line;
            }
        }
        root.stateStr = getStateToString(root);

        try {
            System.gc();
            Solution solution = aStar(root, initialStateBuilder.toString(), heuristic);
            if (Objects.nonNull(solution)) {
                solution.print(heuristic);
            } else {
                System.out.println("Can't find solution: solution = null");
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Can't find solution: OutOfMemoryError");
        }
    }

    public static class Node {
        int[] state = new int[16];
        String stateStr;
        int emptyIndex;
        int level = 0;      // g(n)
        int hCalculated;    // h(n)
        String movement = "";
        Node root = null;
        Node up = null;
        Node down = null;
        Node left = null;
        Node right = null;

        public void printState() {
            System.out.println(" ---------------");
            for (int line = 0; line < 4; line++) {
                System.out.print("| ");
                for (int column = 0; column < 4; column++) {
                    printNumber(state[4 * line + column]);
                }
                System.out.print("\n");
            }
            System.out.print(" ---------------\n\n");
        }
    }

    public static void printNumber(int number) {
        StringBuilder builder;
        if (number > 0) {
            builder = new StringBuilder(Integer.toString(number));
        } else {
            builder = new StringBuilder();
        }
        while (builder.length() < 2) {
            builder.append(" ");
        }
        System.out.print(builder + "| ");
    }

    public static class Solution {
        int visitedNodeCount = 0;
        Node node = null;
        long memoryUsed;
        long totalTime;
        String steps = "";
        String initialState = "";

        public void print(int heuristic) {
            System.out.print("H'[" + heuristic + "] ");
            System.out.print("Movements[" + steps.split(" ").length + "] ");
            System.out.print("Nodes[" + visitedNodeCount + "] ");
            System.out.print("Memory[" + memoryUsed / 1000 + "mb] ");
            System.out.print("Time[" + totalTime + "ms] ");
            long days = TimeUnit.MILLISECONDS.toDays(totalTime);
            totalTime -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(totalTime);
            totalTime -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime);
            totalTime -= TimeUnit.MINUTES.toMillis(minutes);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(totalTime);
            totalTime -= TimeUnit.SECONDS.toMillis(seconds);
            System.out.println("TimeFormatted[" + days + "d:" + hours + "h:" + minutes + "m:" + seconds + "s:" + totalTime + "ms]");
        }
    }

    public static Solution aStar(Node root, String initialState, int heuristic) {
        List<Node> nonVisitedNodeList = new ArrayList<>();
        Node currentNode = null;
        int visitedNodeCount = 0;
        long startTime = System.currentTimeMillis();

        root.hCalculated = getHeuristicValue(root, heuristic);
        nonVisitedNodeList.add(root);
        while (visitedNodeCount < Integer.MAX_VALUE) {

            int minValue = Integer.MAX_VALUE;
            for (Node currentUnexpandedNode : nonVisitedNodeList) {
                // verificando min(f(n)), onde f(n) = h(n) + g(n))
                if (currentUnexpandedNode.hCalculated + currentUnexpandedNode.level < minValue) {
                    minValue = currentUnexpandedNode.hCalculated + currentUnexpandedNode.level;
                    currentNode = currentUnexpandedNode;
                }
            }
            // A <- A - {v}
            nonVisitedNodeList.remove(currentNode);

            // verificando se v E T
            if (Objects.requireNonNull(currentNode).hCalculated == 0) {
                return createSolution(initialState, visitedNodeCount, startTime, currentNode);
            } else {
                // F <- F U {v}
                // não achei necessário guardar o nó que foi consumido
                visitedNodeCount++;

                // gerando sucessores e calculando h(n) e g(n)
                checkMovementsAndCreateChild(currentNode, heuristic);
                if (Objects.nonNull(currentNode.down)) {
                    // linha 9 e 10 -> versão II
                    // line9And10VersionII(nonVisitedNodeList, currentNode.down);
                    // A <- A U {m}
                    nonVisitedNodeList.add(currentNode.down);
                }
                if (Objects.nonNull(currentNode.up)) {
                    // linha 9 e 10 -> versão II
                    // line9And10VersionII(nonVisitedNodeList, currentNode.up);
                    // A <- A U {m}
                    nonVisitedNodeList.add(currentNode.up);
                }
                if (Objects.nonNull(currentNode.left)) {
                    // linha 9 e 10 -> versão II
                    // line9And10VersionII(nonVisitedNodeList, currentNode.left);
                    // A <- A U {m}
                    nonVisitedNodeList.add(currentNode.left);
                }
                if (Objects.nonNull(currentNode.right)) {
                    // linha 9 e 10 -> versão II
                    // line9And10VersionII(nonVisitedNodeList, currentNode.right);
                    // A <- A U {m}
                    nonVisitedNodeList.add(currentNode.right);
                }
            }
        }
        return null;
    }

    public static void line9And10VersionII(List<Node> nonVisitedNodeList, Node node) {
        Optional<Node> nodeOptional =
                nonVisitedNodeList
                        .stream()
                        .filter(n -> n.stateStr.equals(node.stateStr))
                        .findFirst();

        if (nodeOptional.isPresent()) {
            Node nodeFound = nodeOptional.get();
            if (node.hCalculated + node.level < nodeFound.hCalculated + nodeFound.level) {
                nonVisitedNodeList.remove(nodeOptional.get());
            }
        }
    }

    public static void checkMovementsAndCreateChild(Node root, int heuristic) {
        if (root.emptyIndex < 12) {
            root.down = new Node();
            root.down.movement = "D";
            root.down.level = root.level + 1;
            root.down.emptyIndex = root.emptyIndex + 4;
            root.down.root = root;
            root.down.state = executeMovement(root.state, root.emptyIndex, 'D');
            root.down.stateStr = getStateToString(root.down);
            root.down.hCalculated = getHeuristicValue(root.down, heuristic);
        }
        if (root.emptyIndex > 3) {
            root.up = new Node();
            root.up.movement = "U";
            root.up.level = root.level + 1;
            root.up.emptyIndex = root.emptyIndex - 4;
            root.up.root = root;
            root.up.state = executeMovement(root.state, root.emptyIndex, 'U');
            root.up.stateStr = getStateToString(root.up);
            root.up.hCalculated = getHeuristicValue(root.up, heuristic);
        }
        if (root.emptyIndex != 0 && root.emptyIndex != 4 && root.emptyIndex != 8 && root.emptyIndex != 12) {
            root.left = new Node();
            root.left.movement = "L";
            root.left.level = root.level + 1;
            root.left.emptyIndex = root.emptyIndex - 1;
            root.left.root = root;
            root.left.state = executeMovement(root.state, root.emptyIndex, 'L');
            root.left.stateStr = getStateToString(root.left);
            root.left.hCalculated = getHeuristicValue(root.left, heuristic);
        }
        if (root.emptyIndex != 3 && root.emptyIndex != 7 && root.emptyIndex != 11 && root.emptyIndex != 15) {
            root.right = new Node();
            root.right.movement = "R";
            root.right.level = root.level + 1;
            root.right.emptyIndex = root.emptyIndex + 1;
            root.right.root = root;
            root.right.state = executeMovement(root.state, root.emptyIndex, 'R');
            root.right.stateStr = getStateToString(root.right);
            root.right.hCalculated = getHeuristicValue(root.right, heuristic);
        }
    }

    public static String getStateToString(Node node) {
        return Arrays
                .stream(node.state)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" "));
    }

    public static int getHeuristicValue(Node node, int heuristic) {
        switch (heuristic) {
            case 2:
                return calculateH2(node);
            case 3:
                return calculateH3(node);
            case 4:
                return calculateH4(node);
            case 5:
                return calculateH5(node);
            default:
                return calculateH1(node);
        }

    }

    // 1. h'1(n) = número de peças foras de seu lugar na configuração final.
    public static int calculateH1(Node root) {
        int count = 0;
        for (int index = 0; index < 16; index++) {
            if (STATE_ACCEPTABLE[index] != root.state[index]) {
                count++;
            }
        }
        return count;
    }

    // 2. h’2(n) = número de peças fora de ordem na sequência numérica das 15 peças, seguindo a ordem das posições no tabuleiro.
    public static int calculateH2(Node root) {
        int count = 0;
        for (int column = 0; column < 4; column++) {
            for (int line = 0; line < 4; line++) {
                int index = line * 4 + column;
                int actualValue = root.state[index];
                int last;
                if (index != 0) {
                    if (index > 3) {
                        last = root.state[index - 4];
                    } else {
                        last = root.state[index + 11];
                    }

                    if (actualValue != 0 && actualValue != last + 1) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /*
        3. h’3(n) = para cada peça fora de seu lugar somar a distância Manhattam (quantidade de
                    deslocamentos) para colocar em seu devido lugar. Neste caso considera-se que o
                    caminho esteja livre para fazer o menor número de movimentos.
    */
    public static int calculateH3(Node root) {
        int manhattanSum = 0;
        int currentValue;
        for (int line = 0; line < 4; line++) {
            for (int column = 0; column < 4; column++) {
                currentValue = root.state[line * 4 + column];

                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        if (STATE_ACCEPTABLE[i * 4 + j] == currentValue) {
                            manhattanSum += Math.abs(i - line) + Math.abs(j - column);
                        }
                    }
                }
            }
        }
        return manhattanSum;
    }

    /*
        4. h’4(n) = p1*h'1(n) + p2*h’2(n) + p3*h’3(n), sendo p1 + p2 + p3 são pesos (número real) tais
                    que p1 + p2 + p3 = 1. A escolha desses pesos deverão ser escolhidos conforme os
                    resultado dos experimentos.
    */
    public static int calculateH4(Node root) {
        BigDecimal pH1 = BigDecimal.valueOf(0.20);
        BigDecimal pH2 = BigDecimal.valueOf(0.20);
        BigDecimal pH3 = BigDecimal.valueOf(0.60);
        return pH1
                .multiply(BigDecimal.valueOf(calculateH1(root)))
                .add(pH2
                        .multiply(BigDecimal.valueOf(calculateH2(root)))
                        .add(pH3
                                .multiply(BigDecimal.valueOf(calculateH3(root)))))
                .intValue();
    }

    // 5. h’5(n) = max(h'1(n), h’2(n), h’3(n)).
    public static int calculateH5(Node root) {
        return Arrays
                .stream(new int[]{calculateH1(root), calculateH2(root), calculateH3(root)})
                .max()
                .getAsInt();
    }


    public static int[] executeMovement(int[] rootState, int emptyLine, char movementDirection) {
        int[] newState = new int[16];
        System.arraycopy(rootState, 0, newState, 0, 16);

        if (movementDirection == 'D') {
            newState[emptyLine] = newState[emptyLine + 4];
            newState[emptyLine + 4] = 0;
        } else if (movementDirection == 'U') {
            newState[emptyLine] = newState[emptyLine - 4];
            newState[emptyLine - 4] = 0;
        } else if (movementDirection == 'L') {
            newState[emptyLine] = newState[emptyLine - 1];
            newState[emptyLine - 1] = 0;
        } else { // R
            newState[emptyLine] = newState[emptyLine + 1];
            newState[emptyLine + 1] = 0;
        }
        return newState;
    }

    public static Solution createSolution(String initialState, int visitedNodeCount, long startTime, Node node) {
        Solution solution = new Solution();
        Runtime runtime = Runtime.getRuntime();

        solution.visitedNodeCount = visitedNodeCount;
        solution.node = node;
        solution.initialState = initialState;
        solution.steps = getPathOfSolution(node);

        solution.memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024);
        solution.totalTime = System.currentTimeMillis() - startTime;

        return solution;
    }

    public static String getPathOfSolution(Node solution) {
        LinkedList<Node> solutionPath = new LinkedList<>();
        Node root = solution;
        StringBuilder path = new StringBuilder();

        while (Objects.nonNull(root)) {
            solutionPath.addFirst(root);
            root = root.root;
        }
        while (!solutionPath.isEmpty()) {
            Node nodeRemoved = solutionPath.removeFirst();
            if (!nodeRemoved.movement.equals("Start")) {
                switch (nodeRemoved.movement) {
                    case "D":
                        path.append("DOWN ");
                        break;
                    case "U":
                        path.append("UP ");
                        break;
                    case "L":
                        path.append("LEFT ");
                        break;
                    case "R":
                        path.append("RIGHT ");
                        break;
                }
            }
        }
        return path.toString();
    }
}