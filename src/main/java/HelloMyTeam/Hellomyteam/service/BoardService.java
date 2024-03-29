package HelloMyTeam.Hellomyteam.service;

import HelloMyTeam.Hellomyteam.dto.*;
import HelloMyTeam.Hellomyteam.entity.Board;
import HelloMyTeam.Hellomyteam.entity.BoardCategory;
import HelloMyTeam.Hellomyteam.entity.TeamMemberInfo;
import HelloMyTeam.Hellomyteam.entity.status.BoardAndCommentStatus;
import HelloMyTeam.Hellomyteam.repository.BoardRepository;
import HelloMyTeam.Hellomyteam.repository.LikeRepository;
import HelloMyTeam.Hellomyteam.repository.TeamMemberInfoRepository;
import HelloMyTeam.Hellomyteam.repository.custom.impl.BoardCustomImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {
    private final static String VIEWCOOKIENAME = "alreadyViewCookie";
    private final BoardRepository boardRepository;
    private final BoardCustomImpl boardCustomImpl;
    private final TeamMemberInfoRepository teamMemberInfoRepository;
    private final LikeRepository likeRepository;
    private final EntityManager em;


    public Board createBoard(BoardWriteDto boardWriteDto) {
        TeamMemberInfo findTeamMemberInfo = teamMemberInfoRepository.findById(boardWriteDto.getTeamMemberInfoId())
                .orElseThrow(()-> new IllegalStateException("teamMemberInfo id가 누락되었습니다."));

        Board board = Board.builder()
                .boardCategory(boardWriteDto.getBoardCategory())
                .writer(findTeamMemberInfo.getMember().getName())
                .title(boardWriteDto.getTitle())
                .contents(boardWriteDto.getContents())
                .boardStatus(BoardAndCommentStatus.NORMAL)
                .teamMemberInfo(findTeamMemberInfo)
                .build();
        return boardRepository.save(board);
    }

//    public Page<BoardListResDto> getBoards(Long teamId, BoardCategory boardCategory, BoardSearchReqDto boardSearchReqDto, BoardSearch2ReqDto boardSearch2ReqDto, Pageable pageable) {
//        //TODO pageable 값 설정 하고 레포에 전달
//        log.info("start service");
////        PageRequest pageRequest = PageRequest.of(15, 15, Sort.by(Sort.Direction.fromString(boardSearchReqDto.getOrderBy()), boardSearchReqDto.getSortBy()));
////        PageRequest pageRequest = PageRequest.of((int) pageable.getOffset(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, boardSearchReqDto.getOrderBy()));
////        PageRequest pageRequest = PageRequest.of((int) pageable.getOffset(), pageable.getPageSize(), Sort.by(boardSearchReqDto.getOrderBy()).descending());
////        PageRequest pageRequest = PageRequest.of((int) pageable.getOffset(), pageable.getPageSize(),
////                Sort.by(Sort.Direction.fromString(boardSearchReqDto.getOrderBy()), boardSearch2ReqDto.getSortBy()));
//        PageRequest pageRequest = PageRequest.of((int) pageable.getOffset(), pageable.getPageSize(),
//                Sort.by(Sort.Direction.fromString(boardSearchReqDto.getOrderBy()), String.valueOf(pageable.getSort())));
//        log.info("page request");
//        Page<BoardListResDto> boards = boardCustomImpl.findBoardsByTeamId(teamId, boardCategory, boardSearchReqDto, pageRequest);
//        return boards;
//    }

    public BoardDetailResDto getBoard(Long id) {
        BoardDetailResDto findBoard = boardCustomImpl.findBoardById(id);
        return findBoard;
    }

    public Board updateBoard(Long boardId, BoardUpdateDto boardUpdateDto) {
        Board findBoard = em.find(Board.class, boardId);
        findBoard.setBoardCategory(boardUpdateDto.getChangeBoardCategory());
        findBoard.setContents(boardUpdateDto.getChangeContents());
        findBoard.setTitle(boardUpdateDto.getChangeTitle());
        return findBoard;
    }

    public void deleteBoard(Long boardId) {
        boardRepository.deleteById(boardId);
    }

    public int updateView(Long boardId, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        boolean checkCookie = false;
        int result = 0;
        if(cookies != null){
            for (Cookie cookie : cookies)
            {
                if (cookie.getName().equals(VIEWCOOKIENAME+boardId)) checkCookie = true;

            }
            if(!checkCookie){
                Cookie newCookie = createCookieForForNotOverlap(boardId);
                response.addCookie(newCookie);
                result = boardCustomImpl.updateView(boardId);
            }
        } else {
            Cookie newCookie = createCookieForForNotOverlap(boardId);
            response.addCookie(newCookie);
            result = boardCustomImpl.updateView(boardId);
        }
        return result;
    }

    //TODO utils로 옮기기
    private Cookie createCookieForForNotOverlap(Long boardId) {
        Cookie cookie = new Cookie(VIEWCOOKIENAME+boardId, String.valueOf(boardId));
        cookie.setComment("조회수 중복 증가 방지 쿠키");	// 쿠키 용도 설명 기재
        cookie.setMaxAge(getRemainSecondForTommorow()); 	// 쿠키유지 : 하루
        cookie.setHttpOnly(true);				// 서버에서만 조작 가능
        return cookie;
    }
    // 다음 날 정각까지 남은 시간(초)
    private int getRemainSecondForTommorow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tommorow = LocalDateTime.now().plusDays(1L).truncatedTo(ChronoUnit.DAYS);
        return (int) now.until(tommorow, ChronoUnit.SECONDS);
    }

    public Board getBoardById(Long boardId) {
        Board board = boardRepository.getBoardById(boardId);
        return board;
    }

//    public Board getBoard(Long id) {
//        Board board = boardRepository.findById(id).get();
//        return board;
//    }
}
