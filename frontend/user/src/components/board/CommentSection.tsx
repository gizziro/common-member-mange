"use client";

import { useEffect, useState, useCallback } from "react";
import { apiGet, apiPost, apiPut, apiDelete } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  ThumbsUp,
  ThumbsDown,
  ArrowBendDownRight,
  PencilSimple,
  Trash,
} from "@phosphor-icons/react";

/* ===========================
 * 타입 정의
 * =========================== */

/** 댓글 응답 DTO (트리 구조) */
interface CommentDto {
  id: string;
  postId: string;
  parentId: string | null;
  depth: number;
  content: string;
  authorId: string;
  authorName: string;
  voteUpCount: number;
  voteDownCount: number;
  isDeleted: boolean;
  children: CommentDto[];
  createdAt: string;
  updatedAt: string;
}

/** 투표 응답 DTO */
interface VoteResponse {
  voteUpCount: number;
  voteDownCount: number;
  userVoteType: string | null;
}

/** 컴포넌트 Props */
interface CommentSectionProps {
  /** 게시판 인스턴스 ID */
  boardId: string;
  /** 게시글 ID */
  postId: string;
  /** 댓글 권한 */
  permissions: Record<string, string[]>;
  /** 현재 로그인 사용자 ID (null이면 비로그인) */
  currentUserId: string | null;
}

/* ===========================
 * 댓글 섹션 컴포넌트
 * 트리 구조 댓글 목록 + 작성/수정/삭제/투표
 * =========================== */

export function CommentSection({ boardId, postId, permissions, currentUserId }: CommentSectionProps) {
  const [comments, setComments] = useState<CommentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 인증 토큰
  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") ?? undefined : undefined;

  // 댓글 작성 권한 확인
  const canWrite = permissions?.comment?.includes("write") ?? false;

  // 댓글 목록 로드
  const loadComments = useCallback(async () => {
    try {
      const res = await apiGet<CommentDto[]>(
        `/boards/${boardId}/posts/${postId}/comments`,
        token
      );
      if (res.success && res.data) {
        setComments(res.data);
      }
    } catch {
      // 댓글 로드 실패 — 무시
    } finally {
      setLoading(false);
    }
  }, [boardId, postId, token]);

  useEffect(() => {
    loadComments();
  }, [loadComments]);

  // 새 댓글 작성
  const handleSubmitComment = async () => {
    if (!newComment.trim() || submitting) return;
    setSubmitting(true);
    try {
      const res = await apiPost(
        `/boards/${boardId}/posts/${postId}/comments`,
        { content: newComment },
        token
      );
      if (res.success) {
        setNewComment("");
        await loadComments();
      }
    } catch {
      // 작성 실패
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold">댓글 {comments.length > 0 && `(${countAll(comments)})`}</h3>

      {/* 새 댓글 작성 폼 */}
      {canWrite && currentUserId && (
        <div className="space-y-2">
          <textarea
            className="w-full rounded-lg border p-3 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary min-h-[80px] resize-y"
            placeholder="댓글을 입력하세요..."
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
          />
          <div className="flex justify-end">
            <Button size="sm" disabled={!newComment.trim() || submitting} onClick={handleSubmitComment}>
              {submitting ? "등록 중..." : "댓글 등록"}
            </Button>
          </div>
        </div>
      )}

      {!canWrite && !currentUserId && (
        <p className="text-sm text-muted-foreground">로그인 후 댓글을 작성할 수 있습니다.</p>
      )}

      {/* 댓글 목록 (트리) */}
      {comments.length === 0 ? (
        <p className="py-4 text-center text-sm text-muted-foreground">댓글이 없습니다.</p>
      ) : (
        <div className="space-y-1">
          {comments.map((c) => (
            <CommentNode
              key={c.id}
              comment={c}
              boardId={boardId}
              postId={postId}
              permissions={permissions}
              currentUserId={currentUserId}
              token={token}
              onRefresh={loadComments}
              depth={0}
            />
          ))}
        </div>
      )}
    </div>
  );
}

/* ===========================
 * 개별 댓글 노드 (재귀 렌더링)
 * =========================== */

interface CommentNodeProps {
  comment: CommentDto;
  boardId: string;
  postId: string;
  permissions: Record<string, string[]>;
  currentUserId: string | null;
  token: string | undefined;
  onRefresh: () => Promise<void>;
  depth: number;
}

function CommentNode({ comment, boardId, postId, permissions, currentUserId, token, onRefresh, depth }: CommentNodeProps) {
  const [replyOpen, setReplyOpen] = useState(false);
  const [replyContent, setReplyContent] = useState("");
  const [replying, setReplying] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [editSaving, setEditSaving] = useState(false);

  const canWrite = permissions?.comment?.includes("write") ?? false;
  const canModify = permissions?.comment?.includes("modify") ?? false;
  const canDelete = permissions?.comment?.includes("delete") ?? false;
  const isOwner = currentUserId === comment.authorId;

  // 답글 작성
  const handleReply = async () => {
    if (!replyContent.trim() || replying) return;
    setReplying(true);
    try {
      const res = await apiPost(
        `/boards/${boardId}/posts/${postId}/comments`,
        { content: replyContent, parentId: comment.id },
        token
      );
      if (res.success) {
        setReplyContent("");
        setReplyOpen(false);
        await onRefresh();
      }
    } catch {
      // 답글 실패
    } finally {
      setReplying(false);
    }
  };

  // 댓글 수정
  const handleEdit = async () => {
    if (!editContent.trim() || editSaving) return;
    setEditSaving(true);
    try {
      const res = await apiPut(
        `/boards/${boardId}/comments/${comment.id}`,
        { content: editContent },
        token
      );
      if (res.success) {
        setEditing(false);
        await onRefresh();
      }
    } catch {
      // 수정 실패
    } finally {
      setEditSaving(false);
    }
  };

  // 댓글 삭제
  const handleDelete = async () => {
    try {
      const res = await apiDelete(
        `/boards/${boardId}/comments/${comment.id}`,
        token
      );
      if (res.success) {
        await onRefresh();
      }
    } catch {
      // 삭제 실패
    }
  };

  // 투표
  const handleVote = async (voteType: "UP" | "DOWN") => {
    if (!currentUserId) return;
    try {
      await apiPost<VoteResponse>(
        `/boards/${boardId}/comments/${comment.id}/vote`,
        { voteType },
        token
      );
      await onRefresh();
    } catch {
      // 투표 실패
    }
  };

  // 날짜 포맷
  const formatDate = (d: string) =>
    new Date(d).toLocaleString("ko-KR", {
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit",
    });

  return (
    <div style={{ marginLeft: depth * 24 }} className="py-2">
      <div className="rounded-lg border p-3">
        {/* 헤더: 작성자 + 시간 */}
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span className="font-medium text-foreground">{comment.authorName}</span>
          <span>{formatDate(comment.createdAt)}</span>
          {comment.isDeleted && <Badge variant="secondary">삭제됨</Badge>}
          {comment.updatedAt !== comment.createdAt && !comment.isDeleted && (
            <span className="text-muted-foreground">(수정됨)</span>
          )}
        </div>

        {/* 본문 */}
        {comment.isDeleted ? (
          <p className="mt-2 text-sm italic text-muted-foreground">삭제된 댓글입니다.</p>
        ) : editing ? (
          <div className="mt-2 space-y-2">
            <textarea
              className="w-full rounded-lg border p-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary min-h-[60px] resize-y"
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
            />
            <div className="flex gap-1">
              <Button size="xs" disabled={editSaving} onClick={handleEdit}>
                {editSaving ? "저장 중..." : "저장"}
              </Button>
              <Button size="xs" variant="ghost" onClick={() => { setEditing(false); setEditContent(comment.content); }}>
                취소
              </Button>
            </div>
          </div>
        ) : (
          <p className="mt-2 text-sm whitespace-pre-wrap">{comment.content}</p>
        )}

        {/* 액션 버튼 */}
        {!comment.isDeleted && !editing && (
          <div className="mt-2 flex items-center gap-2">
            {/* 투표 */}
            {currentUserId && (
              <>
                <button
                  onClick={() => handleVote("UP")}
                  className="flex items-center gap-1 text-xs text-muted-foreground hover:text-primary transition-colors"
                >
                  <ThumbsUp size={14} />
                  <span>{comment.voteUpCount}</span>
                </button>
                <button
                  onClick={() => handleVote("DOWN")}
                  className="flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive transition-colors"
                >
                  <ThumbsDown size={14} />
                  <span>{comment.voteDownCount}</span>
                </button>
              </>
            )}

            {/* 답글 */}
            {canWrite && currentUserId && (
              <button
                onClick={() => setReplyOpen(!replyOpen)}
                className="flex items-center gap-1 text-xs text-muted-foreground hover:text-primary transition-colors"
              >
                <ArrowBendDownRight size={14} />
                답글
              </button>
            )}

            {/* 수정 (본인 + 수정 권한) */}
            {isOwner && canModify && (
              <button
                onClick={() => setEditing(true)}
                className="flex items-center gap-1 text-xs text-muted-foreground hover:text-primary transition-colors"
              >
                <PencilSimple size={14} />
                수정
              </button>
            )}

            {/* 삭제 (본인 또는 삭제 권한) */}
            {(isOwner || canDelete) && (
              <button
                onClick={handleDelete}
                className="flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive transition-colors"
              >
                <Trash size={14} />
                삭제
              </button>
            )}
          </div>
        )}

        {/* 답글 폼 */}
        {replyOpen && (
          <div className="mt-3 space-y-2 border-t pt-3">
            <textarea
              className="w-full rounded-lg border p-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary min-h-[60px] resize-y"
              placeholder="답글을 입력하세요..."
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
            />
            <div className="flex gap-1">
              <Button size="xs" disabled={!replyContent.trim() || replying} onClick={handleReply}>
                {replying ? "등록 중..." : "답글 등록"}
              </Button>
              <Button size="xs" variant="ghost" onClick={() => setReplyOpen(false)}>취소</Button>
            </div>
          </div>
        )}
      </div>

      {/* 하위 댓글 재귀 렌더링 */}
      {comment.children && comment.children.length > 0 && (
        <div className="mt-1">
          {comment.children.map((child) => (
            <CommentNode
              key={child.id}
              comment={child}
              boardId={boardId}
              postId={postId}
              permissions={permissions}
              currentUserId={currentUserId}
              token={token}
              onRefresh={onRefresh}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}

/** 댓글 총 개수 세기 (재귀) */
function countAll(comments: CommentDto[]): number {
  return comments.reduce((acc, c) => acc + 1 + countAll(c.children ?? []), 0);
}
