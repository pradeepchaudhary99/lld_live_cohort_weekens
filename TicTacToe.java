

/*

0. Understand the question and make yourself familair with problem..
    TicTacToe
    --> 3*3 
    Symbols: X,O
    Players

1. Requirements:
    1. Functional Requirments:
        - should be able to take turns and put their symbol on the board
        - Board containing 3x3
        - turn alternation should be there
        - if there is a match [diagonal, row, col] then declare the winner
        - if there is a draw, if number of pieces are 9 but no winner
    
    2. Non-Functional:
        - skipped
    
2. Indentify the core entities
    - enum Symbol:
        X, O
    - Board
        int numberOfSymbols;
        size
        - hasWinner(i,j,Symbol)
            --> 
        - isDraw()
        - placeSymbol(i,j)
            -- checkIfValid()

        - 
    - Game
        --> orchastrator class running the game and turns
        List<Player>
        Board
        start()
            --> turn = 1 - turn; // 
            Player player = players.get(turn);

            int valid = placeSymbol(i,j)
            if(valid != -1)
                continue;

            if(hasWinner())
                break;
            
    - Player
        --> name
        --> Symbol
    

3. Write Code


4. Discuss extensibility..
    --> thread saf





*/


public class TicTacToe {
    
}
