package InternationalChess.pgn;

import InternationalChess.engine.classic.board.Board;
import InternationalChess.engine.classic.board.BoardUtils;
import InternationalChess.engine.classic.board.Move;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static InternationalChess.engine.classic.board.Move.MoveFactory;

public class PGNUtilities {

    private static final Pattern KING_SIDE_CASTLE = Pattern.compile("O-O#?\\+?");
    private static final Pattern QUEEN_SIDE_CASTLE = Pattern.compile("O-O-O#?\\+?");
    private static final Pattern PLAIN_PAWN_MOVE = Pattern.compile("^([a-h][0-8])(\\+)?(#)?$");
    private static final Pattern PAWN_ATTACK_MOVE = Pattern.compile("(^[a-h])(x)([a-h][0-8])(\\+)?(#)?$");
    private static final Pattern PLAIN_MAJOR_MOVE = Pattern.compile("^(B|N|R|Q|K)([a-h]|[1-8])?([a-h][0-8])(\\+)?(#)?$");
    private static final Pattern MAJOR_ATTACK_MOVE = Pattern.compile("^(B|N|R|Q|K)([a-h]|[1-8])?(x)([a-h][0-8])(\\+)?(#)?$");
    private static final Pattern PLAIN_PAWN_PROMOTION_MOVE = Pattern.compile("(.*?)=(.*?)");
    private static final Pattern ATTACK_PAWN_PROMOTION_MOVE = Pattern.compile("(.*?)x(.*?)=(.*?)");

    private PGNUtilities() {
        throw new RuntimeException("Not Instantiable!");
    }

    public static List<String> processMoveText(final String gameText) throws ParsePGNException {
        return gameText.isEmpty() ? Collections.emptyList() : createMovesFromPGN(gameText);
    }

    private static List<String> createMovesFromPGN(final String pgnText) {
        if(!pgnText.startsWith("1.")) {
            return Collections.emptyList();
        }
        final List<String> sanitizedMoves = new LinkedList<>(Arrays.asList(
                removeParenthesis(pgnText).replaceAll(Pattern.quote("$") + "[0-9]+", "").replaceAll("[0-9]+\\s*\\.\\.\\.", "")
                        .split("\\s*[0-9]+" + Pattern.quote("."))));
        final List<String> processedData = removeEmptyText(sanitizedMoves);
        final String[] moveRows = processedData.toArray(new String[processedData.size()]);
        final ImmutableList.Builder<String> moves = new Builder<>();
        for(final String row : moveRows) {
            final String[] moveContent = removeWhiteSpace(row).split(" ");
            if(moveContent.length == 1) {
                moves.add(moveContent[0]);
            } else if(moveContent.length == 2) {
                moves.add(moveContent[0]);
                moves.add(moveContent[1]);
            } else {
                System.out.println("problem reading: " +pgnText+ " skipping!");
                return Collections.emptyList();
            }
        }
        return moves.build();
    }

    private static List<String> removeEmptyText(final List<String> moves) {
        final List<String> result = new ArrayList<>();
        for(final String moveText : moves) {
            if(!moveText.isEmpty()) {
                result.add(moveText);
            }
        }
        return result;
    }

    private static String removeWhiteSpace(final String row) {
        return row.trim().replaceAll("\\s+", " ");
    }

    private static String removeParenthesis(final String gameText) {
        int parenthesisCounter=0;
        final StringBuilder builder = new StringBuilder();
        for (final char c : gameText.toCharArray()) {
            if (c == '(' || c == '{' ) {
                parenthesisCounter++;
            }
            if (c == ')' || c == '}' ) {
                parenthesisCounter--;
            }
            if (!(c == '(' || c == '{' || c == ')' || c == '}') && parenthesisCounter == 0) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static Move createMove(final Board board,
                                  final String pgnText) {

        final Matcher kingSideCastleMatcher = KING_SIDE_CASTLE.matcher(pgnText);
        final Matcher queenSideCastleMatcher = QUEEN_SIDE_CASTLE.matcher(pgnText);
        final Matcher plainPawnMatcher = PLAIN_PAWN_MOVE.matcher(pgnText);
        final Matcher attackPawnMatcher = PAWN_ATTACK_MOVE.matcher(pgnText);
        final Matcher pawnPromotionMatcher = PLAIN_PAWN_PROMOTION_MOVE.matcher(pgnText);
        final Matcher attackPawnPromotionMatcher = ATTACK_PAWN_PROMOTION_MOVE.matcher(pgnText);
        final Matcher plainMajorMatcher = PLAIN_MAJOR_MOVE.matcher(pgnText);
        final Matcher attackMajorMatcher = MAJOR_ATTACK_MOVE.matcher(pgnText);

        int currentCoordinate;
        int destinationCoordinate;

        if(kingSideCastleMatcher.matches()) {
            return extractCastleMove(board, "O-O");
        } else if (queenSideCastleMatcher.matches()) {
            return extractCastleMove(board, "O-O-O");
        } else if(plainPawnMatcher.matches()) {
            final String destinationSquare = plainPawnMatcher.group(1);
            destinationCoordinate = BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
            currentCoordinate = deriveCurrentCoordinate(board, "P", destinationSquare, "");
            return MoveFactory.createMove(board, currentCoordinate, destinationCoordinate);
        } else if(attackPawnMatcher.matches()) {
            final String destinationSquare = attackPawnMatcher.group(3);
            destinationCoordinate = BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
            final String disambiguationFile = attackPawnMatcher.group(1) != null ? attackPawnMatcher.group(1) : "";
            currentCoordinate = deriveCurrentCoordinate(board, "P", destinationSquare, disambiguationFile);
            return MoveFactory.createMove(board, currentCoordinate, destinationCoordinate);
        } else if (attackPawnPromotionMatcher.matches()) {
            final String destinationSquare = attackPawnPromotionMatcher.group(2);
            final String disambiguationFile = attackPawnPromotionMatcher.group(1) != null ? attackPawnPromotionMatcher.group(1) : "";
            destinationCoordinate = BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
            currentCoordinate = deriveCurrentCoordinate(board, "P", destinationSquare, disambiguationFile);
            return MoveFactory.createMove(board, currentCoordinate, destinationCoordinate);
        } else if(pawnPromotionMatcher.find()) {
            final String destinationSquare = pawnPromotionMatcher.group(1);
            destinationCoordinate = BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
            currentCoordinate = deriveCurrentCoordinate(board, "P", destinationSquare, "");
            return MoveFactory.createMove(board, currentCoordinate, destinationCoordinate);
        } else if (plainMajorMatcher.find()) {
            final String destinationSquare = plainMajorMatcher.group(3);
            destinationCoordinate = BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
            final String disambiguationFile = plainMajorMatcher.group(2) != null ? plainMajorMatcher.group(2) : "";
            currentCoordinate = deriveCurrentCoordinate(board, plainMajorMatcher.group(1), destinationSquare, disambiguationFile);
            return MoveFactory.createMove(board, currentCoordinate, destinationCoordinate);
        } else if(attackMajorMatcher.find()) {
            final String destinationSquare = attackMajorMatcher.group(4);
            destinationCoordinate = BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
            final String disambiguationFile = attackMajorMatcher.group(2) != null ? attackMajorMatcher.group(2) : "";
            currentCoordinate = deriveCurrentCoordinate(board, attackMajorMatcher.group(1), destinationSquare, disambiguationFile);
            return MoveFactory.createMove(board, currentCoordinate, destinationCoordinate);
        }

        return MoveFactory.getNullMove();

    }

    private static Move extractCastleMove(final Board board,
                                          final String castleMove) {
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            if (move.isCastlingMove() && move.toString().equals(castleMove)) {
                return move;
            }
        }
        return MoveFactory.getNullMove();
    }

    private static int deriveCurrentCoordinate(final Board board,
                                               final String movedPiece,
                                               final String destinationSquare,
                                               final String disambiguationFile) throws RuntimeException {
        final List<Move> currentCandidates = new ArrayList<>();
        final int destinationCoordinate =  BoardUtils.INSTANCE.getCoordinateAtPosition(destinationSquare);
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            if (move.getDestinationCoordinate() == destinationCoordinate && move.getMovedPiece().toString().equals(movedPiece)) {
                currentCandidates.add(move);
            }
        }

        if(currentCandidates.size() == 0) {
            return -1;
        }

        return currentCandidates.size() == 1
                ? currentCandidates.iterator().next().getCurrentCoordinate()
                : extractFurther(currentCandidates, movedPiece, disambiguationFile);

    }

    private static int extractFurther(final List<Move> candidateMoves,
                                      final String movedPiece,
                                      final String disambiguationFile) {

        final List<Move> currentCandidates = new ArrayList<>();

        for(final Move move : candidateMoves) {
            if(move.getMovedPiece().getPieceType().toString().equals(movedPiece)) {
                currentCandidates.add(move);
            }
        }

        if(currentCandidates.size() == 1) {
            return currentCandidates.iterator().next().getCurrentCoordinate();
        }

        final List<Move> candidatesRefined = new ArrayList<>();

        for (final Move move : currentCandidates) {
            final String pos = BoardUtils.INSTANCE.getPositionAtCoordinate(move.getCurrentCoordinate());
            if (pos.contains(disambiguationFile)) {
                candidatesRefined.add(move);
            }
        }

        if(candidatesRefined.size() == 1) {
            return candidatesRefined.iterator().next().getCurrentCoordinate();
        }

        return -1;

    }

}
