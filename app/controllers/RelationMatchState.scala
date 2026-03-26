package controllers

enum RelationMatchState(val index: Int):
  case MATCHED_BOTH extends RelationMatchState(0)
  case MATCHED_SOURCE_NODE_ONLY extends RelationMatchState(1) 
  case MATCHED_TARGET_NODE_ONLY extends RelationMatchState(2)
  case NOT_MATCHED_BOTH extends RelationMatchState(3)
