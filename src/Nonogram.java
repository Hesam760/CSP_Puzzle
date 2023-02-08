import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

public class Nonogram {

    private State state;
    private int n;
    ArrayList<ArrayList<Integer>> row_constraints;
    ArrayList<ArrayList<Integer>> col_constraints;

    public Nonogram(State state,
                    ArrayList<ArrayList<Integer>> row_constraints,
                    ArrayList<ArrayList<Integer>> col_constraints) {
        this.state = state;
        this.n = state.getN();
        this.row_constraints = row_constraints;
        this.col_constraints = col_constraints;
    }


    public void start() {
        long tStart = System.nanoTime();
        backtrack(state);
        long tEnd = System.nanoTime();
        System.out.println("Total time: " + (tEnd - tStart) / 1000000000.000000000);
    }

    private boolean backtrack(State state) {

        if (isFinished(state)) {
            System.out.println("Result Board: \n");
            state.printBoard();
            return true;
        }
        if (allAssigned(state)) {
            return false;
        }

        int[] mrvRes = MRV(state);
        forwardChecking(mrvRes, state);
        arcFunction(mrvRes, state);
        for (String s : LCV(state, mrvRes)) {
            State newState = state.copy();
            newState.setIndexBoard(mrvRes[0], mrvRes[1], s);
            newState.removeIndexDomain(mrvRes[0], mrvRes[1], s);
            if (!isConsistent(newState)) {
                newState.removeIndexDomain(mrvRes[0], mrvRes[1], s);
                continue;
            }

            if (backtrack(newState)) {
                return true;
            }
        }

        return false;
    }

//    private ArrayList<String> LCV(State state, int[] var) {
//        return state.getDomain().get(var[0]).get(var[1]);
//    }

    private ArrayList<String> LCV(State state, int[] var) {
        ArrayList<String> result = new ArrayList<>();
        int rowCounter = 0, columnCounter = 0, counterI = 0, counterJ = 0;
        double sum = 0, eCounter = 0, fCounter = 0;
        boolean fNPI = false, fNPJ = false;

        for (int index : this.row_constraints.get(var[0])) {
            rowCounter = rowCounter + index;
            sum = sum + rowCounter;
        }

        for (int index : this.col_constraints.get(var[1])) {
            columnCounter = columnCounter + index;
            sum = sum + columnCounter;
        }

        for (int i = 0; i < state.getN(); i++) {
            if (state.getBoard().get(var[0]).get(i).equals("F"))
                counterI++;
            if (state.getBoard().get(i).get(var[1]).equals("F"))
                counterJ++;
        }
        if (counterI < rowCounter)
            fNPI = true;

        if (counterJ < columnCounter && fNPI)
            fNPJ = true;

        for (int index = 0; index < state.getN(); index++) {
            if ((state.getBoard().get(var[0]).get(index).equals("F")) || (state.getBoard().get(index).get(var[1]).equals("F")))
                fCounter++;
            else if ((state.getBoard().get(var[0]).get(index).equals("E")) || (state.getBoard().get(index).get(var[1]).equals("E")))
                eCounter++;
        }

        if (!fNPJ) {
            result.add("X");
            return result;
        }
        if (n >= 20) {
            if (sum - fCounter - eCounter > n) {
                result.add("F");
                result.add("X");
            } else {
                result.add("X");
                result.add("F");
            }
        } else {
            if (sum - fCounter - eCounter > 1) {
                result.add("F");
                result.add("X");
            } else {
                result.add("X");
                result.add("F");
            }
        }
        return result;
    }

    private void arcFunction(int[] result, State state) {

        int countF = 0, countE = 0, value = 0;
        for (int i = 0; i < row_constraints.get(result[0]).size(); i++) {
            for (int j = 0; j < result[1]; j++) {
                switch (state.getBoard().get(result[0]).get(j)) {
                    case "F" -> {
                        countF++;
                    }
                    case "E" -> {
                        countE++;
                    }
                    default -> {
                        countE = 0;
                        countF = 0;
                    }
                }
                if (value < countE + countF) {
                    value = countE + countF;
                }
                if (value < row_constraints.get(result[0]).get(i) && countF > 0) {
                    state.setIndexBoard(result[0], j, "F");
                    state.removeIndexDomain(result[0], j, "X");
                }
            }
        }

        value = 0;
        countE = 0;
        countF = 0;
        for (int j = 0; j < col_constraints.get(result[1]).size(); j++) {
            for (int i = 0; i < result[0]; i++) {
                switch (state.getBoard().get(i).get(result[1])) {
                    case "F" -> {
                        countF++;
                    }
                    case "E" -> {
                        countE++;
                    }
                    default -> {
                        countE = 0;
                        countF = 0;
                    }
                }
                if (value < countE + countF)
                    value = countE + countF;

                if (value < col_constraints.get(result[1]).get(j) && countF > 0) {
                    state.setIndexBoard(i, result[1], "F");
                    state.removeIndexDomain(i, result[1], "X");
                }
            }
        }

        if (row_constraints.get(result[0]).size() < 2 && row_constraints.get(result[0]).get(0) > n / 2) {
            for (int i = row_constraints.get(result[0]).get(0); i > n - row_constraints.get(result[0]).get(0); i--) {
                state.setIndexBoard(result[0], i, "F");
                state.removeIndexDomain(result[0], i, "X");
            }
        } else if (col_constraints.get(result[1]).size() < 2 && col_constraints.get(result[1]).get(0) > n / 2) {
            for (int j = state.getN() - this.col_constraints.get(result[1]).get(0); j < this.col_constraints.get(result[1]).get(0); j++) {
                state.removeIndexDomain(j, result[1], "X");
                state.setIndexBoard(j, result[1], "F");
            }
        }
    }

    private void forwardChecking(int[] result, State state) {

        int countI, index1, countJ, index2, getPositionI = 0, getPositionJ = 0;
        boolean isFinishedI = true, isFinishedJ = true;

        for (int i : row_constraints.get(result[0])) {
            countI = 0;
            for (index1 = getPositionI; index1 < state.getN(); index1++) {
                if (state.getBoard().get(result[0]).get(index1).equals("F")) {
                    countI++;
                } else if (state.getBoard().get(result[0]).get(index1).equals("X") && countI != 0) {
                    break;
                }
            }
            getPositionI = index1;
            if (countI < i)
                isFinishedI = false;
        }

        for (int j : col_constraints.get(result[1])) {
            countJ = 0;
            for (index2 = getPositionJ; index2 < state.getN(); index2++) {
                if (state.getBoard().get(index2).get(result[1]).equals("F")) {
                    countJ++;
                } else if (state.getBoard().get(index2).get(result[1]).equals("X") && countJ != 0) {
                    break;
                }
            }
            getPositionJ = index2;
            if (countJ < j)
                isFinishedJ = false;
        }

        if (isFinishedI && isFinishedJ) {
            for (int i = 0; i < state.getN(); i++) {
                if (state.getBoard().get(result[0]).get(i).equals("E")) {
                    state.setIndexBoard(result[0], i, "X");
                    state.removeIndexDomain(result[0], i, "F");
                }
                if (state.getBoard().get(i).get(result[1]).equals("E")) {
                    state.setIndexBoard(i, result[1], "X");
                    state.removeIndexDomain(i, result[1], "F");
                }
            }
        }
    }

    private int[] MRV(State state) {
        ArrayList<ArrayList<String>> cBoard = state.getBoard();
        ArrayList<ArrayList<ArrayList<String>>> cDomain = state.getDomain();

        int[] result = new int[2];
        int changeState = 3;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (cBoard.get(i).get(j).equals("E")) {
                    int value = cDomain.get(i).get(j).size();
                    if (value < changeState) {
                        result[0] = i;
                        result[1] = j;
                        changeState = value;
                    }
                }
            }
        }

        return result;
    }

    private boolean allAssigned(State state) {
        ArrayList<ArrayList<String>> cBoard = state.getBoard();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                String s = cBoard.get(i).get(j);
                if (s.equals("E"))
                    return false;
            }
        }
        return true;
    }


    private boolean isConsistent(State state) {

        ArrayList<ArrayList<String>> cBoard = state.getBoard();
        //check row constraints
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int x : row_constraints.get(i)) {
                sum += x;
            }
            int count_f = 0;
            int count_e = 0;
            int count_x = 0;
            for (int j = 0; j < n; j++) {
                if (cBoard.get(i).get(j).equals("F")) {
                    count_f++;
                } else if (cBoard.get(i).get(j).equals("E")) {
                    count_e++;
                } else if (cBoard.get(i).get(j).equals("X")) {
                    count_x++;
                }
            }

            if (count_x > n - sum) {
                return false;
            }
            if (count_f != sum && count_e == 0) {
                return false;
            }

            Queue<Integer> constraints = new LinkedList<>();
            constraints.addAll(row_constraints.get(i));
            int count = 0;
            boolean flag = false;
            for (int j = 0; j < n; j++) {
                if (cBoard.get(i).get(j).equals("F")) {
                    flag = true;
                    count++;
                } else if (cBoard.get(i).get(j).equals("E")) {
                    break;
                } else if (cBoard.get(i).get(j).equals("X")) {
                    if (flag) {
                        flag = false;
                        if (!constraints.isEmpty()) {
                            if (count != constraints.peek()) {
                                return false;
                            }
                            constraints.remove();
                        }
                        count = 0;
                    }
                }
            }

        }

        //check col constraints

        for (int j = 0; j < n; j++) {
            int sum = 0;
            for (int x : col_constraints.get(j)) {
                sum += x;
            }
            int count_f = 0;
            int count_e = 0;
            int count_x = 0;
            for (int i = 0; i < n; i++) {
                if (cBoard.get(i).get(j).equals("F")) {
                    count_f++;
                } else if (cBoard.get(i).get(j).equals("E")) {
                    count_e++;
                } else if (cBoard.get(i).get(j).equals("X")) {
                    count_x++;
                }
            }
            if (count_x > n - sum) {
                return false;
            }
            if (count_f != sum && count_e == 0) {
                return false;
            }

            Queue<Integer> constraints = new LinkedList<>();
            constraints.addAll(col_constraints.get(j));
            int count = 0;
            boolean flag = false;
            for (int i = 0; i < n; i++) {
                if (cBoard.get(i).get(j).equals("F")) {
                    flag = true;
                    count++;
                } else if (cBoard.get(i).get(j).equals("X")) {
                    if (flag) {
                        flag = false;
                        if (!constraints.isEmpty()) {
                            if (count != constraints.peek()) {
                                return false;
                            }
                            constraints.remove();
                        }
                        count = 0;
                    }
                }
            }
        }
        return true;
    }

    private boolean isFinished(State state) {
        return allAssigned(state) && isConsistent(state);
    }

}














