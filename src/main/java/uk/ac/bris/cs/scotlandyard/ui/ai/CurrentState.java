package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;

import javax.annotation.Nonnull;

public interface CurrentState {
    @Nonnull Board returnBoard();
}
