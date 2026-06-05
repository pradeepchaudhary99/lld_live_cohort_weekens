package week_4_5_6_7;

import java.util.*;
/*
    Functional Requirements
        Initialize chess board - 8x8 board with pieces in starting positions
        Move pieces - Each piece moves according to its type
        Capture pieces - Remove opponent piece when moving to that square
        Detect check - King is under attack
        Detect checkmate - King in check + no legal moves
        Detect stalemate - Not in check + no legal moves
        Turn management - Alternate between WHITE and BLACK
*/
// ══════════════════════════════════════════════════════════════════════════════
// ENUMS
// ══════════════════════════════════════════════════════════════════════════════

enum Color { WHITE, BLACK }
enum PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

// ══════════════════════════════════════════════════════════════════════════════
// POSITION
// ══════════════════════════════════════════════════════════════════════════════

class Position {
    final int row, col;

    Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    boolean isValid() { return row >= 0 && row < 8 && col >= 0 && col < 8; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position p)) return false;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() { return Objects.hash(row, col); }

    @Override
    public String toString() { return "(" + row + "," + col + ")"; }
}

// ══════════════════════════════════════════════════════════════════════════════
// PIECE — Base class
// ══════════════════════════════════════════════════════════════════════════════

abstract class Piece {
    final PieceType type;
    final Color color;

    Piece(PieceType type, Color color) {
        this.type = type;
        this.color = color;
    }

    // Return all possible moves (don't care about check)
    abstract List<Position> getPossibleMoves(Position pos, Board board);

    @Override
    public String toString() {
        return color.name().charAt(0) + "-" + type.name().charAt(0);
    }
}

// ── Pawn ──────────────────────────────────────────────────────────────────────
class Pawn extends Piece {
    Pawn(Color color) { super(PieceType.PAWN, color); }

    @Override
    List<Position> getPossibleMoves(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int dir = color == Color.WHITE ? 1 : -1;
        int startRow = color == Color.WHITE ? 1 : 6;

        // Forward 1
        Position next = new Position(pos.row + dir, pos.col);
        if (next.isValid() && board.isEmpty(next)) {
            moves.add(next);

            // Forward 2 from start
            if (pos.row == startRow) {
                Position next2 = new Position(pos.row + 2 * dir, pos.col);
                if (board.isEmpty(next2)) moves.add(next2);
            }
        }

        // Capture diagonal
        for (int dc : new int[]{-1, 1}) {
            Position cap = new Position(pos.row + dir, pos.col + dc);
            if (cap.isValid() && board.hasOpponent(cap, color)) {
                moves.add(cap);
            }
        }

        return moves;
    }
}

// ── Knight ────────────────────────────────────────────────────────────────────
class Knight extends Piece {
    Knight(Color color) { super(PieceType.KNIGHT, color); }

    @Override
    List<Position> getPossibleMoves(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] moves_list = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};

        for (int[] m : moves_list) {
            Position next = new Position(pos.row + m[0], pos.col + m[1]);
            if (next.isValid() && !board.hasFriendly(next, color)) {
                moves.add(next);
            }
        }
        return moves;
    }
}

// ── Bishop ────────────────────────────────────────────────────────────────────
class Bishop extends Piece {
    Bishop(Color color) { super(PieceType.BISHOP, color); }

    @Override
    List<Position> getPossibleMoves(Position pos, Board board) {
        return slidingMoves(pos, board, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
    }

    protected List<Position> slidingMoves(Position pos, Board board, int[][] dirs) {
        List<Position> moves = new ArrayList<>();
        for (int[] dir : dirs) {
            int r = pos.row + dir[0];
            int c = pos.col + dir[1];

            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                Position next = new Position(r, c);
                if (board.isEmpty(next)) {
                    moves.add(next);
                } else if (board.hasOpponent(next, color)) {
                    moves.add(next);
                    break;
                } else {
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return moves;
    }
}

// ── Rook ──────────────────────────────────────────────────────────────────────
class Rook extends Piece {
    Rook(Color color) { super(PieceType.ROOK, color); }

    @Override
    List<Position> getPossibleMoves(Position pos, Board board) {
        Bishop b = new Bishop(color);
        return b.slidingMoves(pos, board, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
    }
}

// ── Queen ─────────────────────────────────────────────────────────────────────
class Queen extends Piece {
    Queen(Color color) { super(PieceType.QUEEN, color); }

    @Override
    List<Position> getPossibleMoves(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        Bishop b = new Bishop(color);
        moves.addAll(b.slidingMoves(pos, board, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}));
        moves.addAll(b.slidingMoves(pos, board, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}));
        return moves;
    }
}

// ── King ──────────────────────────────────────────────────────────────────────
class King extends Piece {
    King(Color color) { super(PieceType.KING, color); }

    @Override
    List<Position> getPossibleMoves(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

        for (int[] d : dirs) {
            Position next = new Position(pos.row + d[0], pos.col + d[1]);
            if (next.isValid() && !board.hasFriendly(next, color)) {
                moves.add(next);
            }
        }
        return moves;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BOARD
// ══════════════════════════════════════════════════════════════════════════════

class Board {
    private Piece[][] board = new Piece[8][8];

    void init() {
        // White
        board[0][0] = new Rook(Color.WHITE);
        board[0][1] = new Knight(Color.WHITE);
        board[0][2] = new Bishop(Color.WHITE);
        board[0][3] = new Queen(Color.WHITE);
        board[0][4] = new King(Color.WHITE);
        board[0][5] = new Bishop(Color.WHITE);
        board[0][6] = new Knight(Color.WHITE);
        board[0][7] = new Rook(Color.WHITE);

        for (int i = 0; i < 8; i++) board[1][i] = new Pawn(Color.WHITE);

        // Black
        for (int i = 0; i < 8; i++) board[6][i] = new Pawn(Color.BLACK);

        board[7][0] = new Rook(Color.BLACK);
        board[7][1] = new Knight(Color.BLACK);
        board[7][2] = new Bishop(Color.BLACK);
        board[7][3] = new Queen(Color.BLACK);
        board[7][4] = new King(Color.BLACK);
        board[7][5] = new Bishop(Color.BLACK);
        board[7][6] = new Knight(Color.BLACK);
        board[7][7] = new Rook(Color.BLACK);
    }

    Piece get(Position pos) { return board[pos.row][pos.col]; }

    void move(Position from, Position to) {
        board[to.row][to.col] = board[from.row][from.col];
        board[from.row][from.col] = null;
    }

    boolean isEmpty(Position pos) { return get(pos) == null; }

    boolean hasFriendly(Position pos, Color color) {
        Piece p = get(pos);
        return p != null && p.color == color;
    }

    boolean hasOpponent(Position pos, Color color) {
        Piece p = get(pos);
        return p != null && p.color != color;
    }

    Position findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return new Position(r, c);
                }
            }
        }
        return null;
    }

    void display() {
        System.out.println("\n  0 1 2 3 4 5 6 7");
        for (int r = 7; r >= 0; r--) {
            System.out.print(r + " ");
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                System.out.print((p != null ? p.toString() : "..") + " ");
            }
            System.out.println(r);
        }
        System.out.println("  0 1 2 3 4 5 6 7\n");
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CHESS GAME
// ══════════════════════════════════════════════════════════════════════════════

class ChessGame {
    private Board board;
    private Color currentTurn;
    private String status;

    ChessGame() {
        board = new Board();
        board.init();
        currentTurn = Color.WHITE;
        status = "ACTIVE";
    }

    // ── Is king in check? ─────────────────────────────────────────────────────
    boolean isKingInCheck(Color color) {
        Position kingPos = board.findKing(color);
        if (kingPos == null) return false;

        Color opponent = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        // Check all opponent pieces
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.get(new Position(r, c));
                if (p != null && p.color == opponent) {
                    List<Position> moves = p.getPossibleMoves(new Position(r, c), board);
                    if (moves.contains(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Get legal moves (excluding moves that leave king in check) ───────────
    List<Position> getLegalMoves(Position from) {
        Piece piece = board.get(from);
        if (piece == null || piece.color != currentTurn) return new ArrayList<>();

        List<Position> legal = new ArrayList<>();
        List<Position> possible = piece.getPossibleMoves(from, board);

        for (Position to : possible) {
            // Simulate
            Piece captured = board.get(to);
            board.move(from, to);

            // Check if king safe
            if (!isKingInCheck(piece.color)) {
                legal.add(to);
            }

            // Undo
            board.move(to, from);
            if (captured != null) {
                board.board[to.row][to.col] = captured;
            }
        }

        return legal;
    }

    // ── Has any legal moves? ──────────────────────────────────────────────────
    boolean hasLegalMoves(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece p = board.get(pos);
                if (p != null && p.color == color) {
                    currentTurn = color; // temporarily
                    if (!getLegalMoves(pos).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Make move ─────────────────────────────────────────────────────────────
    boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        Position from = new Position(fromRow, fromCol);
        Position to = new Position(toRow, toCol);

        List<Position> legal = getLegalMoves(from);
        if (!legal.contains(to)) {
            System.out.println("[ERROR] Illegal move");
            return false;
        }

        // Execute move
        Piece captured = board.get(to);
        board.move(from, to);

        System.out.println("[MOVE] " + currentTurn + ": " + from + " → " + to 
            + (captured != null ? " (captured " + captured + ")" : ""));

        // Switch turn
        currentTurn = currentTurn == Color.WHITE ? Color.BLACK : Color.WHITE;

        // Update status
        if (isKingInCheck(currentTurn)) {
            if (!hasLegalMoves(currentTurn)) {
                status = "CHECKMATE";
                System.out.println("[GAME OVER] CHECKMATE! " 
                    + (currentTurn == Color.WHITE ? Color.BLACK : Color.WHITE) + " wins!");
            } else {
                status = "CHECK";
                System.out.println("[CHECK] " + currentTurn + " is in check!");
            }
        } else if (!hasLegalMoves(currentTurn)) {
            status = "STALEMATE";
            System.out.println("[GAME OVER] STALEMATE! Draw!");
        }

        return true;
    }

    void display() { board.display(); }

    boolean isGameOver() { return status.equals("CHECKMATE") || status.equals("STALEMATE"); }

    void showStats() {
        System.out.println("Turn: " + currentTurn + " | Status: " + status);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN
// ══════════════════════════════════════════════════════════════════════════════

public class Chess_Game_Basic {
    public static void main(String[] args) {
        ChessGame game = new ChessGame();

        System.out.println("═══ CHESS GAME ═══");
        game.display();

        // Fool's Mate (2 moves)
        System.out.println("═══ Fool's Mate ═══");

        game.makeMove(1, 5, 3, 5); // f2-f4
        game.display();

        game.makeMove(6, 4, 4, 4); // e7-e5
        game.display();

        game.makeMove(3, 5, 5, 7); // f4-h6 (blunder)
        game.display();

        game.makeMove(3, 3, 7, 7); // d6-h2 - Queen moves to h2
        game.display();

        game.showStats();

        // Try another game
        System.out.println("\n\n═══ Simple Opening ═══");
        ChessGame game2 = new ChessGame();
        game2.display();

        game2.makeMove(1, 4, 3, 4); // e2-e4
        game2.display();
        game2.showStats();

        game2.makeMove(6, 4, 4, 4); // e7-e5
        game2.display();
        game2.showStats();

        game2.makeMove(0, 6, 2, 5); // g1-f3 (Knight)
        game2.display();
        game2.showStats();
    }
}