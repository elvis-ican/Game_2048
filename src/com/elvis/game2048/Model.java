package com.elvis.game2048;

import java.util.*;

public class Model {
    private static final int FIELD_WIDTH = 4;
    private Tile[][] gameTiles;
    int score;
    int maxTile;
    private Stack<Tile[][]> previousStates = new Stack<>();
    private Stack<Integer> previousScores = new Stack<>();
    private boolean isSaveNeeded = true;

    public Model() {
        resetGameTiles();
    }

    public Tile[][] getGameTiles() { return gameTiles; }

    private List<Tile> getEmptyTiles() {
        List<Tile> list = new ArrayList<>();
        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                if (gameTiles[i][j].isEmpty())
                    list.add(gameTiles[i][j]);
            }
        }
        return list;
    }

    private void addTile() {
        List<Tile> list = getEmptyTiles();
        if (!list.isEmpty()) {
            int k = (int) (Math.random() * list.size());
            int a = Math.random() < 0.9 ? 2 : 4;
            list.get(k).value = a;
        }
    }

    void resetGameTiles() {
        gameTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                gameTiles[i][j] = new Tile();
            }
        }
        addTile();
        addTile();
        score = 0;
        maxTile = 0;
    }

    private boolean consolidateTiles(Tile[] tiles) {
        boolean hasChanges = false;
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 1; j < tiles.length; j++) {
                if (tiles[j].value > 0 && tiles[j-1].value == 0) {
                    tiles[j-1].value = tiles[j].value;
                    tiles[j].value = 0;
                    hasChanges = true;
                }
            }
        }
        return hasChanges;
    }

    private boolean mergeTiles(Tile[] tiles) {
        boolean hasChanges = false;
        for (int i = 0; i < tiles.length - 1; i++) {
            if (tiles[i].value > 0 && tiles[i].value == tiles[i+1].value) {
                int newValue = tiles[i].value * 2;
                tiles[i].value = newValue;
                tiles[i+1].value = 0;
                score += newValue;
                if (maxTile < newValue)
                    maxTile = newValue;
                hasChanges = true;
            }
        }
        consolidateTiles(tiles);
        return hasChanges;
    }

    public void left() {
        if (isSaveNeeded)
            saveState(gameTiles);
        boolean hasChanges = false;
        for (int i = 0; i < FIELD_WIDTH; i++) {
            boolean hasConsolidated = consolidateTiles(gameTiles[i]);
            boolean hasMerged = mergeTiles(gameTiles[i]);
            if (hasConsolidated || hasMerged)
                hasChanges = true;
        }
        if (hasChanges)
            addTile();
        isSaveNeeded = true;
    }

    public void down () {
        saveState(gameTiles);
        rotate90Clockwise();
        left();
        rotate90Clockwise();
        rotate90Clockwise();
        rotate90Clockwise();
    }

    public void right() {
        saveState(gameTiles);
        rotate90Clockwise();
        rotate90Clockwise();
        left();
        rotate90Clockwise();
        rotate90Clockwise();
    }

    public void up() {
        saveState(gameTiles);
        rotate90Clockwise();
        rotate90Clockwise();
        rotate90Clockwise();
        left();
        rotate90Clockwise();
    }

    public void rotate90Clockwise() {
        Tile[][] oriTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                oriTiles[i][j] = new Tile(gameTiles[i][j].value);
            }
        }
        int n = 0;
        for (int i = 0; i < FIELD_WIDTH; i++) {
            int m = FIELD_WIDTH -1;
            for (int j = 0; j < FIELD_WIDTH; j++) {
                gameTiles[i][j].value = oriTiles[m][n].value;
                m--;
            }
            n++;
        }
    }

    public boolean canMove() {
        if (!getEmptyTiles().isEmpty())
            return true;
        else {
            for (int i = 0; i < FIELD_WIDTH; i++) {
                for (int j = 0; j < FIELD_WIDTH - 1; j++) {
                    if (gameTiles[i][j].value == gameTiles[i][j+1].value)
                        return true;
                }
            }
            for (int j = 0; j < FIELD_WIDTH; j++) {
                for (int i = 0; i < FIELD_WIDTH - 1; i++) {
                    if (gameTiles[i][j].value == gameTiles[i+1][j].value)
                        return true;
                }
            }
        }
        return false;
    }

    private void saveState(Tile[][] tiles) {
        Tile[][] savedGameTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                savedGameTiles[i][j] = new Tile(tiles[i][j].value);
            }
        }
        previousStates.push(savedGameTiles);
        previousScores.push(score);
        isSaveNeeded = false;
    }

    public void rollback() {
        if (!previousStates.isEmpty())
            gameTiles = previousStates.pop();
        if (!previousScores.isEmpty())
            score = previousScores.pop();
    }

    public void randomMove() {
        int n = (int)(Math.random()*100) % 4;
        switch(n) {
            case 0: up();
            case 1: down();
            case 2: left();
            case 3: right();
        }
    }

    public boolean hasBoardChanged() {
        return getBoardWeights(previousStates.peek()) != getBoardWeights(gameTiles);
    }

    public int getBoardWeights(Tile[][] tiles) {
        int result = 0;
        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                result += tiles[i][j].value;
            }
        }
        return result;
    }

    public MoveFitness getMoveFitness(Move move) {
        move.move();
        MoveFitness moveFitness;
        if (!hasBoardChanged()) {
            moveFitness = new MoveFitness(-1, 0, move);
        } else {
            moveFitness = new MoveFitness(getEmptyTiles().size(), score, move);
        }
        rollback();
        return moveFitness;
    }

    public void autoMove() {
        PriorityQueue<MoveFitness> pQueue = new PriorityQueue<>(4, Collections.reverseOrder());
        pQueue.offer(getMoveFitness(this::left));
        pQueue.offer(getMoveFitness(this::right));
        pQueue.offer(getMoveFitness(this::up));
        pQueue.offer(getMoveFitness(new Move() {
            @Override
            public void move() {
                down();
            }
        }));
        pQueue.poll().getMove().move();
    }
}
