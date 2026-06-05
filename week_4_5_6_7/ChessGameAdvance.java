package week_4_5_6_7;
import java.util.*;

// ══════════════════════════════════════════════════════════════════════════════
// ENUMS
// ══════════════════════════════════════════════════════════════════════════════
/*
Chess Game - Functional Requirements
What the system should do:

    Initialize Board

    Set up all 16 pieces per side in correct positions
    Pawns on rank 2 (white) and rank 7 (black)
    Back rank: Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook


    Move Pieces

    Validate piece exists at source position
    Validate it's correct player's turn
    Validate move is legal (piece-specific movement rules)
    Update piece position
    Capture opponent pieces if target occupied


    Enforce Movement Rules

    Pawn: move forward 1 (or 2 from start), capture diagonally
    Knight: L-shaped moves (2+1 squares)
    Bishop: diagonal moves any distance
    Rook: straight moves any distance (rank/file)
    Queen: combination of bishop + rook
    King: 1 square in any direction


    Detect Check/Checkmate/Stalemate

    Check: King under attack
    Checkmate: King in check + no legal moves
    Stalemate: Not in check + no legal moves
    Draw: Stalemate


    Validate Legal Moves

    Cannot move into check (own king safety)
    Detect all possible moves per piece
    Filter out moves that leave king under attack


    Track Move History

    Record all moves made
    Display move notation

*/
enum Color { WHITE, BLACK }
enum PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
enum GameStatus { ACTIVE, CHECK, CHECKMATE, STALEMATE, DRAW }

// ══════════════════════════════════════════════════════════════════════════════
// POSITION — 0-7 for rank and file (0=a1, 7=h8)
// ══════════════════════════════════════════════════════════════════════════════

class Position {
    final int rank;  // 0-7 (row)
    final int file;  // 0-7 (column)

    Position(int rank, int file) {
        if (rank < 0 || rank > 7 || file < 0 || file > 7) {
            throw new IllegalArgumentException("Invalid position");
        }
        this.rank = rank;
        this.file = file;
    }

    static Position fromString(String pos) {
        // "a1" → (0,0), "h8" → (7,7)
        return new Position(pos.charAt(1) - '1', pos.charAt(0) - 'a');
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position p)) return false;
        return rank == p.rank && file == p.file;
    }

    @Override
    public int hashCode() { return Objects.hash(rank, file); }

    @Override
    public String toString() {
        return (char) ('a' + file) + "" + (rank + 1);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PIECE — Chess piece abstraction
// ══════════════════════════════════════════════════════════════════════════════

abstract class Piece {
    final PieceType type;
    final Color color;

    Piece(PieceType type, Color color) {
        this.type = type;
        this.color = color;
    }

    // Get all possible moves (including illegal ones like moving into check)
    abstract List<Position> getPossibleMoves(Position current, Board board);

    // Utility: get direction from two positions
    protected static int[] getDirection(Position from, Position to) {
        int rankDiff = Integer.signum(to.rank - from.rank);
        int fileDiff = Integer.signum(to.file - from.file);
        return new int[]{rankDiff, fileDiff};
    }

    // Utility: check if path is clear between two positions
    protected static boolean isPathClear(Position from, Position to, Board board) {
        int[] dir = getDirection(from, to);
        int rank = from.rank + dir[0];
        int file = from.file + dir[1];

        while (rank != to.rank || file != to.file) {
            if (board.getPiece(new Position(rank, file)) != null) {
                return false;
            }
            rank += dir[0];
            file += dir[1];
        }
        return true;
    }

    @Override
    public String toString() {
        return color.toString().charAt(0) + type.toString().substring(0, 1);
    }
}

// ── PAWN ─────────────────────────────────────────────────────────────────────
class Pawn extends Piece {
    Pawn(Color color) { super(PieceType.PAWN, color); }

    @Override
    List<Position> getPossibleMoves(Position current, Board board) {
        List<Position> moves = new ArrayList<>();
        int direction = color == Color.WHITE ? 1 : -1;

        // Move forward 1 square
        Position oneForward = new Position(current.rank + direction, current.file);
        if (board.isValid(oneForward) && board.getPiece(oneForward) == null) {
            moves.add(oneForward);

            // Move forward 2 squares from starting position
            int startRank = color == Color.WHITE ? 1 : 6;
            if (current.rank == startRank) {
                Position twoForward = new Position(current.rank + 2 * direction, current.file);
                if (board.getPiece(twoForward) == null) {
                    moves.add(twoForward);
                }
            }
        }

        // Capture diagonally
        for (int fileDelta : new int[]{-1, 1}) {
            Position capture = new Position(current.rank + direction, current.file + fileDelta);
            if (board.isValid(capture)) {
                Piece target = board.getPiece(capture);
                if (target != null && target.color != this.color) {
                    moves.add(capture);
                }
            }
        }

        return moves;
    }
}

// ── KNIGHT ───────────────────────────────────────────────────────────────────
class Knight extends Piece {
    Knight(Color color) { super(PieceType.KNIGHT, color); }

    @Override
    List<Position> getPossibleMoves(Position current, Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] offsets = {{2,1}, {2,-1}, {-2,1}, {-2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};

        for (int[] offset : offsets) {
            Position move = new Position(current.rank + offset[0], current.file + offset[1]);
            if (board.isValid(move)) {
                Piece target = board.getPiece(move);
                if (target == null || target.color != this.color) {
                    moves.add(move);
                }
            }
        }
        return moves;
    }
}

// ── BISHOP ───────────────────────────────────────────────────────────────────
class Bishop extends Piece {
    Bishop(Color color) { super(PieceType.BISHOP, color); }

    @Override
    List<Position> getPossibleMoves(Position current, Board board) {
        return slidingMoves(current, board, new int[][]{{1,1}, {1,-1}, {-1,1}, {-1,-1}});
    }

    protected List<Position> slidingMoves(Position current, Board board, int[][] directions) {
        List<Position> moves = new ArrayList<>();
        for (int[] dir : directions) {
            int rank = current.rank + dir[0];
            int file = current.file + dir[1];

            while (rank >= 0 && rank <= 7 && file >= 0 && file <= 7) {
                Position move = new Position(rank, file);
                Piece target = board.getPiece(move);

                if (target == null) {
                    moves.add(move);
                } else if (target.color != this.color) {
                    moves.add(move);
                    break;
                } else {
                    break;
                }

                rank += dir[0];
                file += dir[1];
            }
        }
        return moves;
    }
}

// ── ROOK ─────────────────────────────────────────────────────────────────────
class Rook extends Piece {
    Rook(Color color) { super(PieceType.ROOK, color); }

    @Override
    List<Position> getPossibleMoves(Position current, Board board) {
        Bishop bishop = new Bishop(color);
        return bishop.slidingMoves(current, board, new int[][]{{1,0}, {-1,0}, {0,1}, {0,-1}});
    }
}

// ── QUEEN ────────────────────────────────────────────────────────────────────
class Queen extends Piece {
    Queen(Color color) { super(PieceType.QUEEN, color); }

    @Override
    List<Position> getPossibleMoves(Position current, Board board) {
        Bishop bishop = new Bishop(color);
        List<Position> moves = new ArrayList<>();
        moves.addAll(bishop.slidingMoves(current, board, 
            new int[][]{{1,1}, {1,-1}, {-1,1}, {-1,-1}})); // diagonal
        moves.addAll(bishop.slidingMoves(current, board, 
            new int[][]{{1,0}, {-1,0}, {0,1}, {0,-1}})); // straight
        return moves;
    }
}

// ── KING ─────────────────────────────────────────────────────────────────────
class King extends Piece {
    King(Color color) { super(PieceType.KING, color); }

    @Override
    List<Position> getPossibleMoves(Position current, Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] offsets = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};

        for (int[] offset : offsets) {
            Position move = new Position(current.rank + offset[0], current.file + offset[1]);
            if (board.isValid(move)) {
                Piece target = board.getPiece(move);
                if (target == null || target.color != this.color) {
                    moves.add(move);
                }
            }
        }
        return moves;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BOARD — 8x8 chess board state
// ══════════════════════════════════════════════════════════════════════════════

class Board {
    private final Map<Position, Piece> pieces = new HashMap<>();

    // ── Initialize board ──────────────────────────────────────────────────────
    void initializeBoard() {
        // White pieces (bottom, ranks 0-1)
        placePiece(new Rook(Color.WHITE), new Position(0, 0));
        placePiece(new Knight(Color.WHITE), new Position(0, 1));
        placePiece(new Bishop(Color.WHITE), new Position(0, 2));
        placePiece(new Queen(Color.WHITE), new Position(0, 3));
        placePiece(new King(Color.WHITE), new Position(0, 4));
        placePiece(new Bishop(Color.WHITE), new Position(0, 5));
        placePiece(new Knight(Color.WHITE), new Position(0, 6));
        placePiece(new Rook(Color.WHITE), new Position(0, 7));

        for (int file = 0; file < 8; file++) {
            placePiece(new Pawn(Color.WHITE), new Position(1, file));
        }

        // Black pieces (top, ranks 6-7)
        for (int file = 0; file < 8; file++) {
            placePiece(new Pawn(Color.BLACK), new Position(6, file));
        }

        placePiece(new Rook(Color.BLACK), new Position(7, 0));
        placePiece(new Knight(Color.BLACK), new Position(7, 1));
        placePiece(new Bishop(Color.BLACK), new Position(7, 2));
        placePiece(new Queen(Color.BLACK), new Position(7, 3));
        placePiece(new King(Color.BLACK), new Position(7, 4));
        placePiece(new Bishop(Color.BLACK), new Position(7, 5));
        placePiece(new Knight(Color.BLACK), new Position(7, 6));
        placePiece(new Rook(Color.BLACK), new Position(7, 7));
    }

    void placePiece(Piece piece, Position pos) { pieces.put(pos, piece); }

    Piece getPiece(Position pos) { return pieces.get(pos); }

    void movePiece(Position from, Position to) {
        Piece piece = pieces.remove(from);
        pieces.put(to, piece);
    }

    void removePiece(Position pos) { pieces.remove(pos); }

    boolean isValid(Position pos) {
        return pos.rank >= 0 && pos.rank <= 7 && pos.file >= 0 && pos.file <= 7;
    }

    Position findKing(Color color) {
        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            if (entry.getValue().type == PieceType.KING && entry.getValue().color == color) {
                return entry.getKey();
            }
        }
        return null;
    }

    void display() {
        System.out.println("\n  a b c d e f g h");
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + " ");
            for (int file = 0; file < 8; file++) {
                Piece piece = pieces.get(new Position(rank, file));
                System.out.print((piece != null ? piece.toString() : ".") + " ");
            }
            System.out.println(rank + 1);
        }
        System.out.println("  a b c d e f g h\n");
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MOVE — Represents a move action
// ══════════════════════════════════════════════════════════════════════════════

class Move {
    final Position from;
    final Position to;

    Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() { return from + "-" + to; }
}

// ══════════════════════════════════════════════════════════════════════════════
// GAME — Chess game coordinator
// ══════════════════════════════════════════════════════════════════════════════

class Chess {
    private final Board board = new Board();
    private Color currentTurn = Color.WHITE;
    private GameStatus status = GameStatus.ACTIVE;
    private List<Move> moveHistory = new ArrayList<>();

    Chess() {
        board.initializeBoard();
    }

    // ── Make move ─────────────────────────────────────────────────────────────
    boolean makeMove(String fromStr, String toStr) {
        Position from = Position.fromString(fromStr);
        Position to = Position.fromString(toStr);

        Piece piece = board.getPiece(from);

        // Validation
        if (piece == null) {
            System.out.println("[ERROR] No piece at " + from);
            return false;
        }

        if (piece.color != currentTurn) {
            System.out.println("[ERROR] Not your turn. Current: " + currentTurn);
            return false;
        }

        // Check if move is legal
        List<Position> legalMoves = getLegalMoves(from);
        if (!legalMoves.contains(to)) {
            System.out.println("[ERROR] Illegal move");
            return false;
        }

        // Simulate move
        Piece capturedPiece = board.getPiece(to);
        board.movePiece(from, to);

        // Check if own king in check
        if (isKingInCheck(currentTurn)) {
            board.movePiece(to, from); // undo
            if (capturedPiece != null) board.placePiece(capturedPiece, to);
            System.out.println("[ERROR] Move leaves king in check");
            return false;
        }

        // Execute move
        moveHistory.add(new Move(from, to));
        System.out.println("[MOVE] " + currentTurn + ": " + fromStr + " → " + toStr 
            + (capturedPiece != null ? " (captured " + capturedPiece + ")" : ""));

        // Switch turn and check status
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        updateGameStatus();

        return true;
    }

    // ── Get legal moves (excluding moves that leave king in check) ───────────
    List<Position> getLegalMoves(Position from) {
        Piece piece = board.getPiece(from);
        if (piece == null) return Collections.emptyList();

        List<Position> legal = new ArrayList<>();
        List<Position> possible = piece.getPossibleMoves(from, board);

        for (Position to : possible) {
            // Simulate move
            Piece captured = board.getPiece(to);
            board.movePiece(from, to);

            // Check if king in check
            if (!isKingInCheck(piece.color)) {
                legal.add(to);
            }

            // Undo move
            board.movePiece(to, from);
            if (captured != null) {
                board.placePiece(captured, to);
            }
        }

        return legal;
    }

    // ── Check if king is under attack ─────────────────────────────────────────
    boolean isKingInCheck(Color color) {
        Position kingPos = board.findKing(color);
        if (kingPos == null) return false;

        Color opponent = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;

        // Check if any opponent piece can capture king
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Position pos = new Position(rank, file);
                Piece piece = board.getPiece(pos);
                if (piece != null && piece.color == opponent) {
                    List<Position> moves = piece.getPossibleMoves(pos, board);
                    if (moves.contains(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Check if player has legal moves ───────────────────────────────────────
    boolean hasLegalMoves(Color color) {
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Position pos = new Position(rank, file);
                Piece piece = board.getPiece(pos);
                if (piece != null && piece.color == color) {
                    if (!getLegalMoves(pos).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Update game status ────────────────────────────────────────────────────
    void updateGameStatus() {
        boolean kingInCheck = isKingInCheck(currentTurn);
        boolean hasLegals = hasLegalMoves(currentTurn);

        if (kingInCheck && !hasLegals) {
            status = GameStatus.CHECKMATE;
            System.out.println("[GAME OVER] CHECKMATE! " 
                + ((currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE) + " wins!");
        } else if (!kingInCheck && !hasLegals) {
            status = GameStatus.STALEMATE;
            System.out.println("[GAME OVER] STALEMATE! Draw!");
        } else if (kingInCheck) {
            status = GameStatus.CHECK;
            System.out.println("[CHECK] " + currentTurn + "'s king is in check!");
        } else {
            status = GameStatus.ACTIVE;
        }
    }

    void displayBoard() { board.display(); }

    GameStatus getStatus() { return status; }

    boolean isGameOver() { return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE; }

    void displayStats() {
        System.out.println("\n═══ Game Stats ═══");
        System.out.println("Current Turn: " + currentTurn);
        System.out.println("Status: " + status);
        System.out.println("Moves: " + moveHistory.size());
        System.out.println("Move History: " + moveHistory);
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN — Demonstration
// ══════════════════════════════════════════════════════════════════════════════

public class ChessGameAdvance {
    public static void main(String[] args) {
        Chess game = new Chess();

        System.out.println("═══ CHESS GAME ═══");
        game.displayBoard();

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 1: Fool's Mate (fastest checkmate)
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n═══ SCENARIO 1: Fool's Mate ═══\n");

        // White moves f pawn
        game.makeMove("f2", "f3");
        game.displayBoard();

        // Black moves e pawn
        game.makeMove("e7", "e5");
        game.displayBoard();

        // White moves g pawn (creates weakness)
        game.makeMove("g2", "g4");
        game.displayBoard();

        // Black plays Qh4 checkmate!
        game.makeMove("d8", "h4");
        game.displayBoard();

        game.displayStats();

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 2: Scholar's Mate (4-move checkmate)
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 2: Scholar's Mate ═══\n");

        Chess game2 = new Chess();
        game2.displayBoard();

        // White e4
        game2.makeMove("e2", "e4");
        game2.displayBoard();

        // Black c5
        game2.makeMove("c7", "c5");
        game2.displayBoard();

        // White Bc4
        game2.makeMove("f1", "c4");
        game2.displayBoard();

        // Black d6
        game2.makeMove("d7", "d6");
        game2.displayBoard();

        // White Qh5
        game2.makeMove("d1", "h5");
        game2.displayBoard();

        // Black Nf6
        game2.makeMove("g8", "f6");
        game2.displayBoard();

        // White Qxf7 checkmate
        game2.makeMove("h5", "f7");
        game2.displayBoard();

        game2.displayStats();

        // ═══════════════════════════════════════════════════════════════════
        // SCENARIO 3: Knight moves and captures
        // ═══════════════════════════════════════════════════════════════════
        System.out.println("\n\n═══ SCENARIO 3: Knight Movements ═══\n");

        Chess game3 = new Chess();
        game3.displayBoard();

        // White Nf3
        game3.makeMove("g1", "f3");
        game3.displayBoard();

        // Black Nf6
        game3.makeMove("g8", "f6");
        game3.displayBoard();

        // White Nc3
        game3.makeMove("b1", "c3");
        game3.displayBoard();

        game3.displayStats();
    }
}